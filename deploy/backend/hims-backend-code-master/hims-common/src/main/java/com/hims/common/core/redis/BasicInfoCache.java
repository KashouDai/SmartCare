package com.hims.common.core.redis;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 基础信息 Cache Aside 支持。Redis 仅用于读优化，异常时回源database。
 */
@Component
public class BasicInfoCache
{
    private static final String NULL_VALUE = "__NULL__";
    private static final int NORMAL_TTL_SECONDS = 900;
    private static final int TTL_JITTER_SECONDS = 300;
    private static final int NULL_TTL_SECONDS = 60;

    @Autowired(required = false)
    private RedisCache redisCache;

    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader)
    {
        if (redisCache != null)
        {
            try
            {
                Object cached = redisCache.getCacheObject(key);
                if (NULL_VALUE.equals(cached))
                {
                    return null;
                }
                if (type.isInstance(cached))
                {
                    return type.cast(cached);
                }
            }
            catch (RuntimeException ignored)
            {
                // 缓存异常不得阻断基础信息读取。
            }
        }

        T value = loader.get();
        cacheValue(key, value);
        return value;
    }

    public void evict(String... keys)
    {
        if (redisCache == null || keys == null)
        {
            return;
        }
        for (String key : keys)
        {
            if (key == null)
            {
                continue;
            }
            try
            {
                redisCache.deleteObject(key);
            }
            catch (RuntimeException ignored)
            {
                // database写入结果不因缓存删除失败而回滚。
            }
        }
    }

    public void evictAfterCommit(final String... keys)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter()
            {
                @Override
                public void afterCommit()
                {
                    evict(keys);
                }
            });
            return;
        }
        evict(keys);
    }

    private <T> void cacheValue(String key, T value)
    {
        if (redisCache == null)
        {
            return;
        }
        try
        {
            if (value == null)
            {
                redisCache.setCacheObject(key, NULL_VALUE, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                return;
            }
            int ttl = NORMAL_TTL_SECONDS + ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS + 1);
            redisCache.setCacheObject(key, value, ttl, TimeUnit.SECONDS);
        }
        catch (RuntimeException ignored)
        {
            // 回写失败不影响本次database查询结果。
        }
    }
}
