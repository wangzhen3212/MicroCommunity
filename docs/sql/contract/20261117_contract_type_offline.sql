-- =============================================================
-- 合同类型下线：contract_type / contract_type_spec 两表与合同类型字段清理
-- 方案：.txcode/plan-code/20261117083114/20261117083114_方案.md
-- 执行顺序不可调换：先回填 -> 再删字段 -> 最后删表
-- 执行前务必备份四张表：
--   mysqldump -u<user> -p <db> contract contract_type contract_type_spec contract_type_template > contract_type_offline_bak.sql
-- 执行后需刷新 AppRouteCache 或重启 service-api（清理的接口注册立即生效）
-- =============================================================

-- 1. 模板表新增类型字段（1 房屋出租 / 2 采购 / 3 维保）
-- 注：回填依赖 contract_type.category（由 2026-11-13 增量脚本新增）；若目标库缺该列，先执行
--     ALTER TABLE `contract_type` ADD COLUMN `category` varchar(4) DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `type_name`;
ALTER TABLE `contract_type_template`
  ADD COLUMN `category` varchar(12) NOT NULL DEFAULT '2' COMMENT '1房屋出租 2采购 3维保' AFTER `contract_type_id`;

-- 2. 用原类型的分类回填模板（必须在删除 contract_type 之前执行）
UPDATE `contract_type_template` t
  LEFT JOIN `contract_type` ct ON ct.`contract_type_id` = t.`contract_type_id`
SET t.`category` = IFNULL(ct.`category`, '2')
WHERE t.`status_cd` = '0';

-- 3. 删除模板与类型的关联字段
ALTER TABLE `contract_type_template` DROP COLUMN `contract_type_id`;

-- 4. 删除合同上的类型字段（历史合同凭 template_id 追溯形态）
ALTER TABLE `contract` DROP COLUMN `contract_type`;

-- 5. 变更明细不再保留合同类型快照（原为 NOT NULL，类型表删除后无法回填）
ALTER TABLE `contract_change_plan_detail` DROP COLUMN `contract_type`;

-- 6. 删除废弃表
DROP TABLE IF EXISTS `contract_type_spec`;
DROP TABLE IF EXISTS `contract_type`;

-- 7. 清理字典、接口注册与菜单
DELETE FROM `t_dict` WHERE `table_name` = 'contract_type';

-- 模板接口名同样含 ContractType，故按精确清单清理类型/类型规格接口
DELETE FROM `c_route`
 WHERE `service_id` IN (
   SELECT `service_id` FROM `c_service`
    WHERE `service_code` IN (
      '/contract/saveContractType',
      '/contract/updateContractType',
      '/contract/deleteContractType',
      '/contract/queryContractType',
      '/contract/saveContractTypeSpec',
      '/contract/updateContractTypeSpec',
      '/contract/deleteContractTypeSpec',
      '/contract/queryContractTypeSpec'
    )
 );

DELETE FROM `c_service`
 WHERE `service_code` IN (
   '/contract/saveContractType',
   '/contract/updateContractType',
   '/contract/deleteContractType',
   '/contract/queryContractType',
   '/contract/saveContractTypeSpec',
   '/contract/updateContractTypeSpec',
   '/contract/deleteContractTypeSpec',
   '/contract/queryContractTypeSpec'
 );

-- 类型规格维护页随代码一并下线（菜单不存在时无影响，按需放开）
-- UPDATE `m_menu` SET status_cd = '1' WHERE url LIKE '%contractTypeSpecManage%';
