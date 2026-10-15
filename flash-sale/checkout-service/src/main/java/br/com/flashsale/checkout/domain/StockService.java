package br.com.flashsale.checkout.domain;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "local current = tonumber(redis.call('GET', KEYS[1]) or '-1') " +
                    "if current < tonumber(ARGV[1]) then return -1 end " +
                    "return redis.call('DECRBY', KEYS[1], ARGV[1])", Long.class);
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return 0 end " +
                    "redis.call('INCRBY', KEYS[1], ARGV[2]) " +
                    "redis.call('SADD', KEYS[2], ARGV[1]) " +
                    "return 1", Long.class);

    private final StringRedisTemplate redis;

    public StockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void setAvailable(String productId, long quantity) {
        redis.opsForValue().set(stockKey(productId), Long.toString(quantity));
    }

    public long available(String productId) {
        String value = redis.opsForValue().get(stockKey(productId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public boolean reserve(String productId, int quantity) {
        Long remaining = redis.execute(RESERVE_SCRIPT, List.of(stockKey(productId)), Integer.toString(quantity));
        return remaining != null && remaining >= 0;
    }

    public boolean compensate(String orderId, String productId, int quantity) {
        Long changed = redis.execute(COMPENSATE_SCRIPT,
                List.of(stockKey(productId), "stock:compensated-orders"), orderId, Integer.toString(quantity));
        return changed != null && changed == 1;
    }

    private String stockKey(String productId) {
        return STOCK_KEY_PREFIX + productId;
    }
}