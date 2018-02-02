import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.XMLScriptBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;

/**
 * create by yexc
 */
public class DynamicSqlHandler {


    /**
     *
     * @param sql 原始sql
     * @param q   参数
     * @param DataBaseType 数据库类型
     * @param handlerType  select|insert|update|delete
     * @return   可执行sql
     */
    public String parserDynamicSql(String sql, Map<String, String> q, String DataBaseType,HANDLERTYPE handlerType){
        sql="<"+ handlerType.type +"> " + sql + " </"+ handlerType.type +">";
        XPathParser xPathParser=new XPathParser(sql);
        XNode xNode=xPathParser.evalNodes("select|insert|update|delete").get(0);
        TypeAliasRegistry typeAliasRegistry=new TypeAliasRegistry();
        typeAliasRegistry.resolveAlias("java.util.Map");
        Configuration configuration=new Configuration();
        configuration.setDatabaseId(DataBaseType);
        XMLScriptBuilder builder = new XMLScriptBuilder(configuration,xNode, Map.class);
        SqlSource sqlSource=builder.parseScriptNode();
        return showSql(configuration,sqlSource.getBoundSql(q));
    }
    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                String paramValue = Matcher.quoteReplacement(this.getParameterValue(parameterObject));
                sql = sql.replaceFirst("\\?", paramValue);
            } else {
                String[] sqlParams = sql.split("\\?");

                for(int i = 0; i < sqlParams.length; ++i) {
                    sql = sql.replaceFirst("\\?", "_sql_param_" + i);
                }

                MetaObject metaObject = configuration.newMetaObject(parameterObject);

                for(int i = 0; i < parameterMappings.size(); ++i) {
                    ParameterMapping parameterMapping = (ParameterMapping)parameterMappings.get(i);
                    String propertyName = parameterMapping.getProperty();
                    Object obj;
                    if (metaObject.hasGetter(propertyName)) {
                        obj = metaObject.getValue(propertyName);
                        String paramValue = Matcher.quoteReplacement(this.getParameterValue(obj));
                        sql = sql.replaceFirst("_sql_param_" + i, paramValue);
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("_sql_param_" + i, Matcher.quoteReplacement(this.getParameterValue(obj)));
                    }
                }
            }
        }
        return sql;
    }

    private String getParameterValue(Object obj) {
        String value = "null";
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(2, 2, Locale.CHINA);
            value = "'" + formatter.format((Date)obj) + "'";
        } else if (obj != null) {
            value = obj.toString();
        }

        return value;
    }


    public enum HANDLERTYPE {
        SELECT("select"), INSERT("insert"), UPDATE("update"), DELETE("delete");
        // 成员变量
        private String type;
        // 构造方法
        private HANDLERTYPE(String type) {
            this.type = type;
        }
    }



    //测试
    public static void main(String[] args){
        Map<String, String> q=new HashMap<>();
        q.put("name","yexc");
        q.put("age",null);
        String sql="SELECT * FROM NAME WHERE 1=1  <if test=\"name!=null\">AND NAME =#{name}</if> " +
                "<if test=\"age!=null\">AND AGE =#{age}</if>";
        DynamicSqlHandler dynamicSqlHandler=new DynamicSqlHandler();
        String resultSql=dynamicSqlHandler.parserDynamicSql(sql,q,"oracle", HANDLERTYPE.SELECT);
        System.out.println(resultSql);

    }


}
