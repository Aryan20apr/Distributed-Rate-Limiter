-- KEYS[1]  = rate limit key (hash)
-- ARGV[1]  = capacity (number)
-- ARGV[2]  = refill_rate_per_sec (number)
-- ARGV[3]  = requested_tokens (number, default 1)
-- ARGV[4]  = ttl_seconds (number)
--
-- Returns: { allowed (0|1), remaining (int), retry_after_ms (int) }

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

local time_parts = redis.call('TIME')
local now_ms = tonumber(time_parts[1]) * 1000 + math.floor(tonumber(time_parts[2]) / 1000)

local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local last_refill = tonumber(redis.call('HGET', key, 'last_refill'))


if tokens == nil then
    tokens = capacity
    last_refill = now_ms
end

local elapsed_sec = (now_ms - last_refill) / 1000
if elapsed_sec > 0 then
    tokens = math.min(capacity, tokens + elapsed_sec * refill_rate)
end

local allowed = 0
local retry_after_ms = 0

if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
else
    if refill_rate > 0 then
        retry_after_ms = math.ceil((requested - tokens) / refill_rate * 1000)
    end
end

redis.call('HSET', key, 'tokens', tokens, 'last_refill', now_ms)
redis.call('EXPIRE', key, ttl)

local reset_at_epoch = math.floor((now_ms + retry_after_ms) / 1000)
if allowed == 1 and retry_after_ms == 0 then
    reset_at_epoch = math.floor(now_ms / 1000) + ttl
end
return { allowed, math.floor(tokens), retry_after_ms, reset_at_epoch }