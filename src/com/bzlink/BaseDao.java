package com.bzlink;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * 
 * DAO实现类的超类
 * 
 * @author Amy
 * @date 2018.9
 */
public abstract class BaseDao extends JdbcDaoSupport
{
    protected Logger log = Logger.getLogger(this.getClass());
    @Resource
    public void setJb(JdbcTemplate jb)
    {
        super.setJdbcTemplate(jb);
    }
    
    public void insertOrUpdate(String sql)
    {
        this.getJdbcTemplate().update(sql);
    }
    
    public void insertOrUpdate(String sql, Object[] object)
    {
        int n = this.getJdbcTemplate().update(sql, object);
        // System.out.println(n);
    }
    
    /**
     * 例如 public void update(Person person) { template.update( "update t_person set name=? where id=?",new Object[]
     * {person.getName(), person.getId() }, new int[]{ java.sql.Types.VARCHAR, java.sql.Types.INTEGER}); }
     * 
     * public void insert(Person person) { template.update("insert into t_person(name) values(?)", new Object[]
     * {person.getName()},new int [] {java.sql.Types.VARCHAR}); } <功能详细描述>
     * 
     * @param sql
     * @param object
     * @param type
     * @see [类、类#方法、类#成员]
     */
    public void insertOrUpdate(String sql, Object[] object, int[] type)
    {
        this.getJdbcTemplate().update(sql, object, type);
    }
    
    public void batchInsertOrBatchUpdates(String sql)
    {
        this.getJdbcTemplate().batchUpdate(sql);
    }
    
    public void batchInsertOrBatchUpdates(String sql, List<Object[]> list)
    {
        this.getJdbcTemplate().batchUpdate(sql, list);
    }
    
    public void batchInsertOrBatchUpdates(String sql, List<Object[]> list, int[] type)
    {
        this.getJdbcTemplate().batchUpdate(sql, list, type);
    }
    
    public void batchInsertOrBatchUpdates(String sql, final BatchPreparedStatementSetter pss)
    {
        this.getJdbcTemplate().batchUpdate(sql, pss);
    }
    
    /**
     * 例如 public List<Person> getAllPersons() { return template.query("select * from t_person", new PersonMapper()); }
     * <功能详细描述>
     * 
     * @param sql
     * @return
     * @see [类、类#方法、类#成员]
     */
    public List<Map<String, Object>> queryForList(String sql)
    {
        return this.getJdbcTemplate().queryForList(sql);
    }
    
    public List<Map<String,Object>> queryForList(String sql,List list)
    {
       return this.getJdbcTemplate().queryForList(sql, list.toArray(), 
           new int[]{java.sql.Types.INTEGER,java.sql.Types.INTEGER});
    }
    
    public List<Map<String,Object>> queryForList(String sql,List list, int[] types)
    {
       return this.getJdbcTemplate().queryForList(sql, list.toArray(), 
           types);
    }

    public void delete(String sql)
    {
        this.getJdbcTemplate().update(sql);
    }
    
    /**
     * 例如 public void delete(Integer personId){ template.update("delete from t_person where id=?", new Object[]
     * {personId},new int[] {java.sql.Types. INTEGER}); } <功能详细描述>
     * 
     * @param sql
     * @param object
     * @see [类、类#方法、类#成员]
     */
    public void delete(String sql, Object[] object)
    {
        this.getJdbcTemplate().update(sql, object);
    }
    /**
     * //只查询一列：name
              String sql = "SELECT NAME FROM CUSTOMER WHERE CUST_ID = ?";
              String name = (String)getJdbcTemplate().queryForObject(
              sql, new Object[] { custId }, String.class);
              return name;
     * @param sql
     * @param object
     * @param types
     * @return
     */
    public Object queryForObject(String sql,Object[] object, Class types)
    { 
        return this.getJdbcTemplate().queryForObject(sql, object, types);
    }
}
