
package com.demand.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demand.module.system.entity.SysConfig;
import com.demand.module.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "sys:config:";
    private static final long CACHE_EXPIRE = 24; // 缓存24小时

    /**
     * 获取所有配置（按分组，仅返回前端可见的）
     */
    public Map<String, List<SysConfig>> getAllConfigs() {
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getIsVisible, 1)
                        .orderByAsc(SysConfig::getConfigGroup)
                        .orderByAsc(SysConfig::getSort)
        );

        return configs.stream().collect(Collectors.groupingBy(SysConfig::getConfigGroup));
    }

    /**
     * 根据分组获取配置（仅返回前端可见的）
     */
    public List<SysConfig> getConfigsByGroup(String configGroup) {
        String cacheKey = CACHE_PREFIX + "group:" + configGroup;

        // 检查缓存
        Boolean hasCache = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(hasCache)) {
            return sysConfigMapper.selectByGroup(configGroup);
        }

        List<SysConfig> configs = sysConfigMapper.selectByGroup(configGroup);

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, "", CACHE_EXPIRE, TimeUnit.HOURS);

        return configs;
    }

    /**
     * 根据配置键获取配置值（包括不可见的配置）
     */
    public String getConfigValue(String configKey) {
        String cacheKey = CACHE_PREFIX + "key:" + configKey;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        String value = sysConfigMapper.selectValueByKey(configKey);

        if (value != null) {
            redisTemplate.opsForValue().set(cacheKey, value, CACHE_EXPIRE, TimeUnit.HOURS);
        }

        return value;
    }

    /**
     * 更新配置
     */
    public void updateConfig(Long id, SysConfig config) {
        SysConfig existing = sysConfigMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("配置不存在");
        }

        // 系统内置配置不允许修改某些字段
        if (existing.getIsSystem() == 1) {
            config.setConfigKey(existing.getConfigKey());
            config.setConfigGroup(existing.getConfigGroup());
            config.setIsSystem(existing.getIsSystem());
        }

        config.setId(id);
        sysConfigMapper.updateById(config);

        // 清除缓存
        clearCache(config.getConfigKey(), config.getConfigGroup());

        log.info("系统配置已更新: {}", config.getConfigKey());
    }

    /**
     * 批量更新配置
     */
    public void batchUpdateConfigs(List<SysConfig> configs) {
        for (SysConfig config : configs) {
            SysConfig existing = sysConfigMapper.selectById(config.getId());
            if (existing != null) {
                // 系统内置配置保护
                if (existing.getIsSystem() == 1) {
                    config.setConfigKey(existing.getConfigKey());
                    config.setConfigGroup(existing.getConfigGroup());
                    config.setIsSystem(existing.getIsSystem());
                }

                sysConfigMapper.updateById(config);
                clearCache(config.getConfigKey(), config.getConfigGroup());
            }
        }

        log.info("批量更新系统配置，共{}条", configs.size());
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache(String configKey) {
        String cacheKey = CACHE_PREFIX + "key:" + configKey;
        redisTemplate.delete(cacheKey);

        // 重新加载到缓存
        getConfigValue(configKey);
    }

    /**
     * 清除缓存
     */
    private void clearCache(String configKey, String configGroup) {
        redisTemplate.delete(CACHE_PREFIX + "key:" + configKey);
        redisTemplate.delete(CACHE_PREFIX + "group:" + configGroup);
        redisTemplate.delete(CACHE_PREFIX + "all");
    }
}