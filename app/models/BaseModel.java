package models;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;

import jpaListeners.BaseModelListener;
import play.db.jpa.Model;

/**
 * @author mayan
 */
@MappedSuperclass
@EntityListeners(BaseModelListener.class)
public class BaseModel extends Model {

	public static HashMap<String, Object> lockedMap = new HashMap<String, Object>();
	public final static int size = 20;

	public boolean isDeleted = false;
	@Version
	public long version;

	public Long createTime = System.currentTimeMillis();

	public Long lastModifyTime = System.currentTimeMillis();

	private static final String AND = " and ";
	private static final String FROM = " from ";
	private static final String WHERE = " where ";
	private static final String FROM_WHERE_PATTERN = "from\\s([\\S].*?)\\swhere\\s";
	private static final String CACHE_KEY_LOCK = "lock/";

	public static String defaultCondition() {
		return "isDeleted=false";
	}

	public static String getDefaultContitionSql(String originStr) {
		String originSql = originStr;
		if (StringUtils.containsIgnoreCase(originSql, FROM)) {
			if (StringUtils.containsIgnoreCase(originSql, WHERE)) {
				Pattern pattern = Pattern.compile(FROM_WHERE_PATTERN, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(originSql);
				while (matcher.find()) {
					String tableName = matcher.group(1);
					String string = tableName.contains(" ") ? tableName.substring(tableName.lastIndexOf(' ') + 1) + '.'
							: "";
					String newSqlString = string + defaultCondition() + AND;
					String originString = matcher.group();
					originSql = originSql.replace(originString, originString + newSqlString);
				}
			} else {
				originSql = originSql + WHERE + defaultCondition();
			}
		} else {
			originSql = defaultCondition() + AND + originSql;
		}
		return originSql;
	}

	public void logicDelete() {
		this.isDeleted = true;
		this.save();
	}

	public Date createTime() {
		return new Date(this.createTime);
	}

	public Date lastModifyTime() {
		return new Date(this.lastModifyTime);
	}

	public static boolean isLocked(Class clazz, Model model) {
		if (model.getId() == null)
			return false;

		return lockedMap.containsKey(CACHE_KEY_LOCK + clazz.getName() + "/" + model.getId());
	}

	public static boolean setLock(Class clazz, Model model) {
		if (model.id == null)
			return false;

		String key = CACHE_KEY_LOCK + clazz.getName() + "/" + model.id;
		Long time = System.currentTimeMillis();
		synchronized (clazz) {
			if (!isLocked(clazz, model)) {
				lockedMap.put(key, time);
				return true;
			} else
				return false;
		}
	}

	public static void delLock(Class clazz, Model model) {
		Long id = model.getId();
		if (id == null)
			return;

		lockedMap.remove(CACHE_KEY_LOCK + clazz.getName() + "/" + model.getId());
	}

}
