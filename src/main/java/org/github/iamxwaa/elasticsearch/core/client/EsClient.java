package org.github.iamxwaa.elasticsearch.core.client;

import java.io.Closeable;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

import org.github.iamxwaa.elasticsearch.constants.ClusterConst;
import org.github.iamxwaa.elasticsearch.core.entry.Mapping;
import org.github.iamxwaa.elasticsearch.core.entry.SearchHit;
import org.github.iamxwaa.elasticsearch.core.entry.ElasticsearchConfig;

/**
 * 抽象的es客户端
 * 
 * @author xw
 * 
 *         2020年12月02日
 */
public abstract class EsClient implements SearchClient, DocClient, IndexClient, ClusterConst, Closeable {

    protected Set<String> indexCache = new HashSet<>();
    private ElasticsearchConfig config;

    public EsClient(ElasticsearchConfig config) {
        this.config = config;
    }

    @Override
    public synchronized int executeBatch(int batch) {
        if (this.batchCount() >= batch) {
            return executeBatch();
        }
        return 0;
    }

    @Override
    public synchronized int executeLastBatch() {
        if (batchCount() > 0) {
            return executeBatch();
        }
        return 0;
    }

    @Override
    public synchronized int executeLastBatch(int batch) {
        if (batchCount() >= batch) {
            return executeBatch();
        }
        return 0;
    }

    /**
     * 缓存查询情况
     * 
     * @param index 索引名称
     * @return
     */
    public boolean isIndexExistCache(String index) {
        if (indexCache.contains(index)) {
            return true;
        }
        boolean ex = isIndexExist(index);
        if (ex) {
            indexCache.add(index);
        }
        return ex;
    }

    /**
     * 获取第一个索引模板配置
     * 
     * @param index 索引名称
     * @return
     */
    public Optional<Mapping> getMapping(String index) {
        List<Mapping> list = getMappings(index);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    /**
     * 写入es的数据转为json字符串
     * 
     * @param object 数据
     * @return
     */
    protected String toJSONString(Object object) {
        try (SerializeWriter out = new SerializeWriter(SerializerFeature.QuoteFieldNames,
                SerializerFeature.SkipTransientField, SerializerFeature.WriteEnumUsingName, SerializerFeature.SortField,
                SerializerFeature.WriteDateUseDateFormat)) {
            JSONSerializer serializer = new JSONSerializer(out);
            SimpleDateFormat sdf = new SimpleDateFormat(ClusterConst.UTC_FORMAT);
            sdf.setTimeZone(ClusterConst.UTC_TIME_ZONE);
            serializer.setDateFormat(sdf);
            serializer.write(object);
            return out.toString();
        }
    }

    /**
     * 获取es连接配置
     */
    public ElasticsearchConfig getConfig() {
        return config;
    }

    /**
     * 获取查询数据体
     * 
     * @param hit 查询元信息
     * @return
     */
    public Map<String, Object> buildSource(SearchHit hit) {
        return hit.getSource();
    }

    /**
     * 添加数据到bulk request,id自动生成
     * 
     * @param index  索引
     * @param type   类型
     * @param source 数据
     * @return 当前添加的数据条数
     */
    public int addBatch(String index, String type, Map<String, Object> source) {
        return addBatch(index, type, null, source);
    }
}
