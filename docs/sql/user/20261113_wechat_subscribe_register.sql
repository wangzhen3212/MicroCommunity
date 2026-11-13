-- ============================================================
-- 房产-关注用户（公众号关注用户同步）运行时配置补录(2026-11-13)
-- 执行环境: 物业库(TT)
-- 说明: 2.0 前后端代码已具备
--       wechat.listWechatSubscribe        (service-user / ListWechatSubscribeCmd, GET)
--       wechat.synchronizeWechatSubscribe (service-user / SynchronizeWechatSubscribeCmd, POST)
--       synchronizeWechatSubscribeAdapt   (service-job / 数据总线 business_type_cd=synchronizeWechatSubscribe)
--       本脚本补齐 c_service / c_route / c_business_databus / m_menu / p_privilege / p_privilege_rel
-- 执行后需刷新服务路由缓存(AppRouteCache)或重启 service-api; 重启 service-job 使数据总线生效
-- 幂等: 先 DELETE 再 INSERT; id 按当前基线顺延(如目标库已有更大编号请改为自增或继续顺延)
-- ============================================================

DELETE FROM `c_service` WHERE `service_code` IN (
  'wechat.listWechatSubscribe',
  'wechat.synchronizeWechatSubscribe');
DELETE FROM `c_route` WHERE `service_id` IN ('982026111309000003', '982026111309000004');
DELETE FROM `m_menu` WHERE `m_id` = '702026111300000001';
DELETE FROM `p_privilege` WHERE `p_id` = '502026111300000001';
DELETE FROM `p_privilege_rel` WHERE `p_id` = '502026111300000001';
DELETE FROM `p_privilege_user` WHERE `p_id` = '502026111300000001';
DELETE FROM `c_business_databus` WHERE `databus_id` = '122026111300000001';

-- ---------------------------------------------------------------------------
-- ① 服务定义 c_service (id 接续 2127/2128 → 2129/2130)
-- ---------------------------------------------------------------------------
INSERT INTO `c_service` VALUES ('2129', '982026111309000003', 'wechat.listWechatSubscribe', 'API', '查询公众号关注用户', '1', null, 'CMD', 'http://user-service', 'GET', '60', '3', '8000418002', NOW(), '0');
INSERT INTO `c_service` VALUES ('2130', '982026111309000004', 'wechat.synchronizeWechatSubscribe', 'API', '同步公众号关注用户', '1', null, 'CMD', 'http://user-service', 'POST', '60', '3', '8000418002', NOW(), '0');

-- ---------------------------------------------------------------------------
-- ② 路由关系 c_route (物业端 app 8000418004, GET 用 Q、POST 用 D; id 接续 3109/3110 → 3111/3112)
-- ---------------------------------------------------------------------------
INSERT INTO `c_route` VALUES ('3111', '8000418004', '982026111309000003', 'Q', '1000', 'S', NOW(), '0');
INSERT INTO `c_route` VALUES ('3112', '8000418004', '982026111309000004', 'D', '1000', 'S', NOW(), '0');

-- ---------------------------------------------------------------------------
-- ③ 数据总线(触发 service-job 的 synchronizeWechatSubscribeAdapt; seq 接续 91 → 92)
--    business_type_cd 须与 DatabusDataDto.BUSINESS_TYPE_SYNCHRONIZE_WECHAT_SUBSCRIBE 一致
--    bean_name 须与 @Component(value = "synchronizeWechatSubscribeAdapt") 一致
-- ---------------------------------------------------------------------------
INSERT INTO `c_business_databus` (`databus_id`, `databus_name`, `business_type_cd`, `bean_name`, `seq`, `state`, `create_time`, `status_cd`)
VALUES ('122026111300000001', '获取公众号关注用户', 'synchronizeWechatSubscribe', 'synchronizeWechatSubscribeAdapt', '92', '1001', NOW(), '0');

-- ---------------------------------------------------------------------------
-- ④ 菜单 / 权限：「关注用户」（房产组 800201904006，seq=19）
-- ---------------------------------------------------------------------------
INSERT INTO `m_menu` VALUES ('702026111300000001', '关注用户', '800201904006', '/#/pages/community/wechatSubscribe', '19', '502026111300000001', '公众号关注用户同步与查看', NOW(), '0', 'Y');
INSERT INTO `p_privilege` VALUES ('502026111300000001', '关注用户', '同步并查看公众号关注用户', NOW(), '0', '800900000003', '/#/pages/community/wechatSubscribe', '702026111300000001');

-- 授权：物业端全部有效权限组（rel_id 自增），也可上线后在「角色权限」界面按需勾选/取消
INSERT INTO `p_privilege_rel` (`p_id`, `pg_id`, `create_time`, `status_cd`)
SELECT DISTINCT '502026111300000001', g.`pg_id`, NOW(), '0'
  FROM `p_privilege_group` g
 WHERE g.`domain` = '800900000003' AND g.`status_cd` = '0';
