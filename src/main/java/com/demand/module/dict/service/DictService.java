package com.demand.module.dict.service;

import com.demand.module.dict.entity.Dict;
import com.demand.module.dict.mapper.DictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 数据字典业务逻辑服务
 * <p>
 * 负责字典数据的查询和缓存管理。
 * 集成 Redis 实现字典数据的高速读取，减少数据库压力。
 * </p>
 * 
 * <h3>缓存策略：</h3>
 * <ul>
 *   <li><b>缓存 Key</b>：{@code dict:{type}}，例如 {@code dict:demand_type}</li>
 *   <li><b>缓存有效期</b>：24 小时</li>
 *   <li><b>缓存更新</b>：修改字典数据后需手动调用 {@link #refreshCache} 刷新</li>
 * </ul>
 * 
 * <h3>性能优化：</h3>
 * <p>
 * 字典数据属于"读多写少"的场景，非常适合缓存：
 * <ul>
 *   <li>首次查询：从数据库读取并写入 Redis（耗时 ~10ms）</li>
 *   <li>后续查询：直接从 Redis 读取（耗时 ~1ms）</li>
 *   <li>缓存命中率可达 99% 以上</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    /**
     * 数据字典数据访问层
     */
    private final DictMapper dictMapper;

    /**
     * Redis 模板，用于缓存字典数据
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 字典缓存 Key 前缀
     * <p>
     * 完整 Key 格式：{@code dict:{type}}，例如 {@code dict:priority}
     * </p>
     */
    private static final String DICT_CACHE_PREFIX = "dict:";

    /**
     * 缓存有效期（小时）
     * <p>
     * 默认 24 小时，平衡数据实时性和缓存命中率
     * </p>
     */
    private static final long CACHE_EXPIRE = 24;

    /**
     * 根据类型查询字典列表
     * <p>
     * 查询流程：
     * 1. 尝试从 Redis 缓存中获取数据
     * 2. 如果缓存命中，直接返回
     * 3. 如果缓存未命中，查询数据库并写入缓存
     * 4. 返回字典列表
     * </p>
     * 
     * <p>
     * <b>性能对比</b>：
     * <ul>
     *   <li>缓存命中：~1ms</li>
     *   <li>缓存未命中：~10ms（数据库查询 + 写入缓存）</li>
     * </ul>
     * </p>
     *
     * @param type 字典类型
     * @return 字典列表（按 sort 升序排列）
     */
    public List<Dict> getDictByType(String type) {
        String cacheKey = DICT_CACHE_PREFIX + type;

        // 1. 尝试从缓存中获取
        List<Dict> cached = (List<Dict>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，查询数据库
        List<Dict> dictList = dictMapper.findByType(type);
        
        // 3. 写入缓存（有效期 24 小时）
        redisTemplate.opsForValue().set(cacheKey, dictList, CACHE_EXPIRE, TimeUnit.HOURS);
        
        return dictList;
    }

    /**
     * 根据类型和编码查询字典项
     * <p>
     * 不进行缓存，直接从数据库查询。
     * 适用于单次查询场景，如将状态码转换为中文名称。
     * </p>
     *
     * @param type 字典类型
     * @param code 字典编码
     * @return 字典对象，如果不存在则返回 null
     */
    public Dict getDictByTypeAndCode(String type, Integer code) {
        return dictMapper.findByTypeAndCode(type, code);
    }

    /**
     * 获取所有字典类型
     * <p>
     * 返回系统中所有已启用的字典类型，用于字典管理界面展示。
     * </p>
     *
     * @return 字典类型列表（去重）
     */
    public List<String> getAllTypes() {
        return dictMapper.findAllTypes();
    }

    /**
     * 刷新字典缓存
     * <p>
     * 删除指定类型的 Redis 缓存，并重新从数据库加载最新数据。
     * 适用于后台修改字典数据后同步更新缓存。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>管理员修改了字典项的名称或排序</li>
     *   <li>新增了字典类型或禁用了某个字典项</li>
     * </ul>
     * </p>
     *
     * @param type 字典类型
     */
    public void refreshCache(String type) {
        String cacheKey = DICT_CACHE_PREFIX + type;
        
        // 1. 删除旧缓存
        redisTemplate.delete(cacheKey);
        
        // 2. 重新加载数据并写入缓存
        getDictByType(type);
        
        log.info("字典缓存已刷新: type={}", type);
    }

    /**
     * 清空所有字典缓存
     * <p>
     * 遍历所有字典类型，逐个删除对应的 Redis Key。
     * 适用于系统初始化或批量更新字典数据的场景。
     * </p>
     */
    public void clearAllCache() {
        List<String> types = dictMapper.findAllTypes();
        for (String type : types) {
            redisTemplate.delete(DICT_CACHE_PREFIX + type);
        }
        log.info("所有字典缓存已清空，共 {} 个类型", types.size());
    }
}
