package noear.weed;

import noear.weed.ext.Act1;
import noear.weed.ext.Act2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuety on 14/11/12.
 *
 * #        //不加表空间（table(xxx) : 默认加表空间）
 * $.       //当前表空间
 * $NOW()   //说明这里是一个sql 函数
 * ?...     //说明这里是一个数组或查询结果
 */
public class DbTableQueryBase<T extends DbTableQueryBase>  {

    String _table;
    DbContext _context;
    SQLBuilder _builder;
    boolean _isLog = false;

    public DbTableQueryBase(DbContext context) {
        _context = context;
        _builder = new SQLBuilder();
    }


    public T log(Boolean isLog){
        _isLog = isLog;
        return (T)this;
    }

    public T expre(Act1<T> action){
        action.run((T)this);
        return (T)this;
    }

    protected T table(String table) { //相当于 from
        if(table.startsWith("#")){
            _table = table.replace("#","");
        }else {
            if (table.indexOf('.') > 0)
                _table = table;
            else
                _table = "$." + table;
        }

        return (T) this;
    }

    //使用 ?... 支持数组参数
    public T where(String where, Object... args) {
        _builder.append(" WHERE ").append(where, args);
        return (T)this;
    }

    public T and(String and, Object... args) {
        _builder.append(" AND ").append(and, args);
        return (T)this;
    }

    public T or(String or, Object... args) {
        _builder.append(" OR ").append(or, args);
        return (T)this;
    }

    public T begin() {
        _builder.append(" ( ");
        return (T)this;
    }

    public T end() {
        _builder.append(" ) ");
        return (T)this;
    }

    public T from(String table){
        _builder.append(" FROM ").append(table);
        return (T)this;
    }

    public long insert(IDataItem data) throws SQLException{
        if (data == null || data.count() == 0)
            return 0;

        List<Object> args = new ArrayList<Object>();
        StringBuilder sb = new StringBuilder();

        sb.append(" INSERT INTO ").append(_table).append(" (");
        data.forEach((key,value)->{
            if(value==null)
                return;

            sb.append(_context.field(key)).append(",");
        });

        sb.deleteCharAt(sb.length() - 1);

        sb.append(") ");
        sb.append("VALUES");
        sb.append("(");

        data.forEach((key,value)->{
            if(value==null)
                return;

            if (value instanceof String) {
                String val2 = (String)value;
                if (val2.indexOf('$') == 0) { //说明是SQL函数
                    sb.append(val2.substring(1)).append(",");
                }
                else {
                    sb.append("?,");
                    args.add(value);
                }
            }
            else {
                sb.append("?,");
                args.add(value);
            }
        });
        sb.deleteCharAt(sb.length() - 1);
        sb.append(");");

        _builder.append(sb.toString(), args.toArray());

        return compile().insert();
    }

    public <T> boolean insertList(List<T> valuesList, Act2<T,DataItem> hander) throws SQLException {
        List<DataItem> list2 = new ArrayList<>();

        for (T values : valuesList) {
            DataItem item = new DataItem();
            hander.run(values, item);

            list2.add(item);
        }

        if (list2.size() > 0) {
            return insertList(list2.get(0), list2);
        }else{
            return false;
        }
    }

    public boolean insertList(List<DataItem> valuesList) throws SQLException {
        if (valuesList == null || valuesList.size() == 0)
            return false;

        return insertList(valuesList.get(0), valuesList);
    }

    protected <T extends GetHandler> boolean insertList(IDataItem cols, List<T> valuesList)throws SQLException{
        if(valuesList == null || valuesList.size()==0)
            return false;

        if (cols == null || cols.count() == 0)
            return false;

        List<Object> args = new ArrayList<Object>();
        StringBuilder sb = new StringBuilder();

        sb.append(" INSERT INTO ").append(_table).append(" (");
        for(String key : cols.keys()){
            sb.append(_context.field(key)).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        sb.append(") ");

        sb.append("VALUES");

        for(GetHandler item : valuesList){
            sb.append("(");

            for(String key : cols.keys()){
               Object val = item.get(key);

                if (val instanceof String) {
                    String val2 = (String)val;
                    if (val2.indexOf('$') == 0) { //说明是SQL函数
                        sb.append(val2.substring(1)).append(",");
                    }
                    else {
                        sb.append("?,");
                        args.add(val);
                    }
                }
                else {
                    sb.append("?,");
                    args.add(val);
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(";");

        _builder.append(sb.toString(), args.toArray());

        return compile().execute() > 0;
    }

    public long insert(Act1<IDataItem> fun) throws SQLException
    {
        DataItem item = new DataItem();
        fun.run(item);

        return insert(item);
    }

    public int update(Act1<IDataItem> fun) throws SQLException
    {
        DataItem item = new DataItem();
        fun.run(item);

        return update(item);
    }

    public int update(IDataItem data) throws SQLException{
        if (data == null || data.count() == 0)
            return 0;

        List<Object> args = new ArrayList<Object>();
        StringBuilder sb = new StringBuilder();

        sb.append("UPDATE ").append(_table).append(" SET ");

        data.forEach((key,value)->{
            if(value==null)
                return;

            if (value instanceof String) {
                String val2 = (String)value;
                if (val2.indexOf('$') == 0) {
                    sb.append(_context.field(key)).append("=").append(val2.substring(1)).append(",");
                }
                else {
                    sb.append(_context.field(key)).append("=?,");
                    args.add(value);
                }
            }
            else {
                sb.append(_context.field(key)).append("=?,");
                args.add(value);
            }
        });

        sb.deleteCharAt(sb.length() - 1);

        _builder.insert(sb.toString(), args.toArray());

        return compile().execute();
    }

    public <T> boolean updateList(String pk, List<T> valuesList, Act2<T,DataItem> hander) throws SQLException {
        List<DataItem> list2 = new ArrayList<>();

        for (T values : valuesList) {
            DataItem item = new DataItem();
            hander.run(values, item);

            list2.add(item);
        }

        if (list2.size() > 0) {
            return updateList(pk, list2.get(0), list2);
        }else{
            return false;
        }
    }

    public boolean updateList(String pk, List<DataItem> valuesList) throws SQLException {
        if (valuesList == null || valuesList.size() == 0)
            return false;

        return updateList(pk, valuesList.get(0), valuesList);
    }

    protected <T extends GetHandler> boolean updateList(String pk, IDataItem cols, List<T> valuesList)throws SQLException{
        if(valuesList == null || valuesList.size()==0)
            return false;

        if (cols == null || cols.count() == 0)
            return false;

        List<Object> args = new ArrayList<Object>();
        StringBuilder sb = new StringBuilder();

        sb.append(" INSERT INTO ").append(_table).append(" (");
        for(String key : cols.keys()){
            sb.append(_context.field(key)).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        sb.append(") ");

        sb.append("VALUES");

        for(GetHandler item : valuesList){
            sb.append("(");

            for(String key : cols.keys()){
                Object val = item.get(key);

                if (val instanceof String) {
                    String val2 = (String)val;
                    if (val2.indexOf('$') == 0) { //说明是SQL函数
                        sb.append(val2.substring(1)).append(",");
                    }
                    else {
                        sb.append("?,");
                        args.add(val);
                    }
                }
                else {
                    sb.append("?,");
                    args.add(val);
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(" ON DUPLICATE KEY UPDATE");
        for(String key : cols.keys()){
            if(pk.equals(key))
                continue;

            sb.append(" ").append(key).append("=VALUES(").append(key).append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(";");

        _builder.append(sb.toString(), args.toArray());

        return compile().execute() > 0;
    }

    public int delete() throws SQLException {
        StringBuilder sb  = new StringBuilder();

        sb.append("DELETE ");

        if(_builder.indexOf(" FROM ")<0){
            sb.append(" FROM ").append(_table);
        }else{
            sb.append(_table);
        }

        _builder.insert(sb.toString());

        return compile().execute();
    }

    public T innerJoin(String table) {
        _builder.append(" INNER JOIN ").append(table);
        return (T)this;
    }

    public T leftJoin(String table) {
        _builder.append(" LEFT JOIN ").append(table);
        return (T)this;
    }

    public T on(String on) {
        _builder.append(" ON ").append(on);
        return (T)this;
    }

    public T groupBy(String groupBy) {
        _builder.append(" GROUP BY ").append(groupBy);
        return (T)this;
    }

    public T orderBy(String orderBy) {
        _builder.append(" ORDER BY ").append(orderBy);
        return (T)this;
    }

    public T limit(int start, int rows) {
        _builder.append(" LIMIT " + start + "," + rows + " ");
        return (T)this;
    }

    public T limit(int rows) {
        _builder.append(" LIMIT " + rows + " ");
        return (T)this;
    }

    private int _top = 0;
    public T top(int num) {
        _top = num;
        //_builder.append(" TOP " + num + " ");
        return (T)this;
    }

    public boolean exists() throws SQLException {
        return exists(null);
    }

    public boolean exists(Act1<IQuery> expre) throws SQLException {
        IQuery q = limit(1).select("1");

        if (expre != null) {
            expre.run(q);
        }

        return q.getValue() != null;
    }

    String _hint = null;
    public T hint(String hint){
        _hint = hint;
        return  (T)this;
    }

    public long count() throws SQLException{
        return count("COUNT(*)");
    }

    public long count(String expr) throws SQLException{
        return select(expr).getVariate().longValue(0l);
    }

    public IQuery select(String columns) {

        StringBuilder sb = new StringBuilder();

        //1.构建sql
        if(_hint!=null) {
            sb.append(_hint);
            _hint = null;
        }

        sb.append("SELECT ");

        if(_top>0){
            sb.append(" TOP ").append(_top).append(" ");
        }

        sb.append(columns).append(" FROM ").append(_table);

        _builder.backup();
        _builder.insert(sb.toString());

        IQuery rst = compile();

        _builder.restore();

        return rst;
    }



    protected DbTran _tran = null;
    public T tran(DbTran transaction)
    {
        _tran = transaction;
        return (T)this;
    }

    public T tran()
    {
        _tran = _context.tran();
        return (T)this;
    }


    //编译（成DbQuery）
    private DbQuery compile() {
        DbQuery temp = new DbQuery(_context).sql(_builder);

        _builder.clear();

        if(_tran!=null)
            temp.tran(_tran);

        return temp.onCommandBuilt((cmd)->{
            cmd.tag   = _table;
            cmd.isLog = _isLog;
        });
    }
}
