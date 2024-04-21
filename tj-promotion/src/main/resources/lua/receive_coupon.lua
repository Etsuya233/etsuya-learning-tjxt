-- 参数列表
local cacheKeyPrefix = KEYS[1]
local couponUserKeyPrefix = KEYS[2]
local userId = ARGV[1]
local couponId = ARGV[2]

local cacheKey = cacheKeyPrefix .. couponId
local couponUserKey = couponUserKeyPrefix .. couponId

-- 检查缓存中是否有这个KEY
if(redis.call('exists', cacheKey) == 0) then
	return 1
end

-- 券是否还有剩的
local totalNum = tonumber(redis.call('hget', cacheKey, 'totalNum'))
if(totalNum <= 0) then
	return 2
end

-- 判断抢券是否结束
local time = tonumber(redis.call('time')[1])
local issueEndTime = tonumber(redis.call('hget', cacheKey, 'issueEndTime'))
if(time > issueEndTime) then
	return 3
end

-- 券的用户限制数量
local userLimit = tonumber(redis.call('hget', cacheKey, 'userLimit'))

-- 用户抢券后，用户拥有的数目
local amount = redis.call('hincrby', couponUserKey, userId, 1);

-- 用户券的数量是否大于限制
if(amount > userLimit) then
	return 4
end

-- 减少券的库存
redis.call('hincrby', cacheKey, 'totalNum', -1)
return 0