package com.hims.common.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSONArray;
import com.hims.common.constant.CacheConstants;
import com.hims.common.core.domain.entity.SysDictData;
import com.hims.common.core.redis.RedisCache;
import com.hims.common.utils.spring.SpringUtils;

/**
 * 字典工具类
 *
 *
 */
public class DictUtils
{
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    private static final int DICT_TTL_SECONDS = 1800;

    private static final int DICT_TTL_JITTER_SECONDS = 300;

    /**
     * 设置字典缓存
     *
     * @param key 参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDatas)
    {
        try
        {
            int ttl = DICT_TTL_SECONDS + ThreadLocalRandom.current().nextInt(DICT_TTL_JITTER_SECONDS + 1);
            SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas, ttl, TimeUnit.SECONDS);
        }
        catch (RuntimeException ignored)
        {
            // 字典缓存写入失败不影响database写入结果。
        }
    }

    /**
     * 获取字典缓存
     *
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<SysDictData> getDictCache(String key)
    {
        try
        {
            JSONArray arrayCache = SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
            if (StringUtils.isNotNull(arrayCache))
            {
                return arrayCache.toList(SysDictData.class);
            }
        }
        catch (RuntimeException ignored)
        {
            // 读取失败时由调用方回源database。
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue)
    {
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel)
    {
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);

        if (StringUtils.isNotNull(datas))
        {
            if (StringUtils.containsAny(separator, dictValue))
            {
                for (SysDictData dict : datas)
                {
                    for (String value : dictValue.split(separator))
                    {
                        if (value.equals(dict.getDictValue()))
                        {
                            propertyString.append(dict.getDictLabel()).append(separator);
                            break;
                        }
                    }
                }
            }
            else
            {
                for (SysDictData dict : datas)
                {
                    if (dictValue.equals(dict.getDictValue()))
                    {
                        return dict.getDictLabel();
                    }
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);

        if (StringUtils.containsAny(separator, dictLabel) && StringUtils.isNotEmpty(datas))
        {
            for (SysDictData dict : datas)
            {
                for (String label : dictLabel.split(separator))
                {
                    if (label.equals(dict.getDictLabel()))
                    {
                        propertyString.append(dict.getDictValue()).append(separator);
                        break;
                    }
                }
            }
        }
        else
        {
            for (SysDictData dict : datas)
            {
                if (dictLabel.equals(dict.getDictLabel()))
                {
                    return dict.getDictValue();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 删除指定字典缓存
     *
     * @param key 字典键
     */
    public static void removeDictCache(String key)
    {
        try
        {
            SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
        }
        catch (RuntimeException ignored)
        {
            // 缓存删除失败不影响database写入结果。
        }
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        try
        {
            Collection<String> keys = SpringUtils.getBean(RedisCache.class).keys(CacheConstants.SYS_DICT_KEY + "*");
            SpringUtils.getBean(RedisCache.class).deleteObject(keys);
        }
        catch (RuntimeException ignored)
        {
            // 缓存清理失败不影响database写入结果。
        }
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_DICT_KEY + configKey;
    }
}
