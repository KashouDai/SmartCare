package com.hims.framework.cache;

import com.hims.common.core.redis.BasicInfoCache;
import com.hims.common.core.redis.RedisCache;
import com.hims.info.domain.DoctorVo;
import com.hims.info.domain.HospitalInfo;
import com.hims.info.mapper.DoctorMapper;
import com.hims.info.mapper.HospitalInfoMapper;
import com.hims.info.service.impl.DoctorServiceImpl;
import com.hims.info.service.impl.HospitalInfoServiceImpl;
import com.hims.framework.config.FastJson2JsonRedisSerializer;
import com.hims.system.mapper.SysDeptMapper;
import com.hims.system.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicInfoCacheTest {

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private BasicInfoCache cache;

    @Test
    void 命中缓存时不查询database() {
        HospitalInfo cached = new HospitalInfo();
        cached.setId(1L);
        when(redisCache.getCacheObject("hospital:info:id:1")).thenReturn(cached);
        AtomicInteger loads = new AtomicInteger();

        HospitalInfo result = cache.getOrLoad("hospital:info:id:1", HospitalInfo.class, () -> {
            loads.incrementAndGet();
            return new HospitalInfo();
        });

        assertSame(cached, result);
        assertEquals(0, loads.get());
        verify(redisCache, never()).setCacheObject(any(), any(), anyInt(), any());
    }

    @Test
    void 基础信息经过真实Redis序列化后保留类型() {
        HospitalInfo source = new HospitalInfo();
        source.setId(1L);
        source.setReleaseTime(new Date());
        source.setSuspendTime(new Date());
        FastJson2JsonRedisSerializer<Object> serializer = new FastJson2JsonRedisSerializer<>(Object.class);

        Object restored = serializer.deserialize(serializer.serialize(source));

        assertTrue(restored instanceof HospitalInfo);
        assertEquals(1L, ((HospitalInfo) restored).getId());
    }

    @Test
    void 未命中时回源并写入带抖动的TTL() {
        HospitalInfo loaded = new HospitalInfo();
        loaded.setId(2L);

        HospitalInfo result = cache.getOrLoad("hospital:info:id:2", HospitalInfo.class, () -> loaded);

        assertSame(loaded, result);
        ArgumentCaptor<Integer> ttl = ArgumentCaptor.forClass(Integer.class);
        verify(redisCache).setCacheObject(eq("hospital:info:id:2"), eq(loaded), ttl.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttl.getValue() >= 900 && ttl.getValue() <= 1200);
    }

    @Test
    void 空结果使用短TTL缓存() {
        assertNull(cache.getOrLoad("hospital:info:id:404", HospitalInfo.class, () -> null));

        verify(redisCache).setCacheObject("hospital:info:id:404", "__NULL__", 60, TimeUnit.SECONDS);
    }

    @Test
    void Redis读写异常时返回database结果() {
        HospitalInfo loaded = new HospitalInfo();
        loaded.setId(3L);
        when(redisCache.getCacheObject("hospital:info:id:3")).thenThrow(new RuntimeException("redis down"));
        org.mockito.Mockito.doThrow(new RuntimeException("redis down")).when(redisCache)
                .setCacheObject(any(), any(), anyInt(), any());

        HospitalInfo result = cache.getOrLoad("hospital:info:id:3", HospitalInfo.class, () -> loaded);

        assertSame(loaded, result);
        assertDoesNotThrow(() -> cache.evict("hospital:info:id:3"));
    }

    @Test
    void 医院更新后删除新旧缓存键() {
        HospitalInfoMapper mapper = org.mockito.Mockito.mock(HospitalInfoMapper.class);
        BasicInfoCache basicInfoCache = org.mockito.Mockito.mock(BasicInfoCache.class);
        HospitalInfoServiceImpl service = new HospitalInfoServiceImpl();
        ReflectionTestUtils.setField(service, "hospitalInfoMapper", mapper);
        ReflectionTestUtils.setField(service, "basicInfoCache", basicInfoCache);
        HospitalInfo old = new HospitalInfo();
        old.setHospitalCode("OLD");
        HospitalInfo changed = new HospitalInfo();
        changed.setId(1L);
        changed.setHospitalCode("NEW");
        when(mapper.selectHospitalInfoById(1L)).thenReturn(old);
        when(mapper.updateHospitalInfo(changed)).thenReturn(1);

        service.updateHospitalInfo(changed);

        verify(basicInfoCache).evictAfterCommit("hospital:info:id:1", "hospital:info:code:OLD", "hospital:info:code:NEW");
    }

    @Test
    void 医生更新后删除详情缓存() {
        DoctorMapper mapper = org.mockito.Mockito.mock(DoctorMapper.class);
        BasicInfoCache basicInfoCache = org.mockito.Mockito.mock(BasicInfoCache.class);
        DoctorServiceImpl service = new DoctorServiceImpl();
        ReflectionTestUtils.setField(service, "doctorMapper", mapper);
        ReflectionTestUtils.setField(service, "basicInfoCache", basicInfoCache);
        DoctorVo doctor = new DoctorVo();
        doctor.setUserId(10L);
        when(mapper.update(doctor)).thenReturn(1);

        service.updateUser(doctor);

        verify(basicInfoCache).evictAfterCommit("hospital:doctor:info:10");
    }

    @Test
    void 科室删除后删除详情缓存() {
        SysDeptMapper mapper = org.mockito.Mockito.mock(SysDeptMapper.class);
        BasicInfoCache basicInfoCache = org.mockito.Mockito.mock(BasicInfoCache.class);
        SysDeptServiceImpl service = new SysDeptServiceImpl();
        ReflectionTestUtils.setField(service, "deptMapper", mapper);
        ReflectionTestUtils.setField(service, "basicInfoCache", basicInfoCache);
        when(mapper.deleteDeptById(20L)).thenReturn(1);

        service.deleteDeptById(20L);

        verify(basicInfoCache).evictAfterCommit("hospital:department:info:20");
    }
}
