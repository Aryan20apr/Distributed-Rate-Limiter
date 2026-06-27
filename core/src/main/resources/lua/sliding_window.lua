-- KEYS[1]  = rate limit key (hash)
-- ARGV[1]  = max_requests (number)
-- ARGV[2]  = window_millis (number)
-- ARGV[3]  = ttl_seconds (number)
--
-- Returns: { allowed (0|1), remaining (int), retry_after_ms (int) }


local key = KEYS[1]
local max_requests = tonumber(ARGV[1])
local window_mills = toNumber(ARGV[2])
local ttl = toNumber(ARGV[3])

local time_parts = redis.call('TIME')
local now_ms = tonumber(time_parts[1]) * 1000 + math.floor(tonumber(time_parts[2]) / 1000)

local current_count = tonumber(redis.call('HGET', key, 'current_count')) or 0
local previous_count = tonumber(redis.call('HGET', key, 'previous_count')) or 0
local window_start = tonumber(redis.call('HGET', key, 'window_start'))

if window_start == nil then
    window_start = now_ms
end

local elapsed = now_ms - window_start


if elapsed >= window_millis then
    local windows_passed = math.floor(elapsed / window_millis)
    if windows_passed == 1 then
        previous_count = current_count
    else
        previous_count = 0
    end
    current_count = 0
    window_start = now_ms
    elapsed = 0
end

local weight = elapsed / window_millis
local estimated = math.floor(previous_count * (1 - weight) + current_count)

local allowed = 0
local retry_after_ms = 0
local remaining = 0

if estimated >= max_requests then
    retry_after_ms = window_millis - elapsed
    remaining = 0
else
    current_count = current_count + 1
    allowed = 1
    remaining = max_requests - estimated - 1
    if remaining < 0 then
        remaining = 0
    end
end

redis.call('HSET', key,
    'current_count', current_count,
    'previous_count', previous_count,
    'window_start', window_start)
redis.call('EXPIRE', key, ttl)

return { allowed, remaining, retry_after_ms }