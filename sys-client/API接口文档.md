# MicroCommunity API 接口文档

> 生成时间: 2026-07-10 | 项目版本: V3.0  
> 用途: 上位机(host-computer)集成参考，标注每个接口的全局标识携带方式

---

## 一、全局标识机制说明

### 1.1 appId —— 应用/小区标识
- **来源**: 微信小程序/公众号的 AppID
- **作用**: 标识请求来自哪个小区(community)，一个 appId 对应一个社区
- **携带方式**: 三种

| 方式 | 示例 | 说明 |
|------|------|------|
| **HTTP Header** `APP_ID` 或 `APP-ID` | 所有 `/app/*` 标准请求 | 主要方式，由前端在请求头中携带 |
| **URL Path** | `/app/ext/{appId}/{serviceCode}` | 外部系统对接时用 |
| **Query Param** | `?appId=wxabc123` | OAuth 微信鉴权场景 |

### 1.2 communityId —— 小区ID
- **来源**: 数据库主键，纯数字
- **携带方式**: 仅在**支付回调URL**中直接出现，或通过 appId 间接映射

### 1.3 上位机路由关键
- **appId** 是上位机路由的核心标识，通过 `appId → sessionId` 映射找到对应网关 WebSocket 连接
- 所有 `/app/*` 请求都携带 appId（Header 或 URL），上位机可直接路由
- **支付回调**分两类：有 appId（URL路径中）和无 appId（需从订单号反查）

---

## 二、接口总览

### 分类统计

| 分类 | 数量 | 是否需上位机关注 |
|------|------|:---:|
| 统一网关入口 (AppController) | 8 | ✅ 全部 |
| 登录认证 (OwnerController/PropertyController) | 13 | ✅ 全部 |
| 支付下单 (PaymentController/GoodsController) | 7 | ✅ 需代理 |
| 支付回调通知 (7个渠道) | 12 | ✅ **核心** |
| 微信网关 (WechatGatewayController) | 2 | ✅ 需代理 |
| 工作流 (ActivitiController) | 3 | ⬜ |
| Token/环境/文件等 | 5 | ⬜ |
| 组件调用 (CallComponentController) | 4 | ⬜ |
| 页面路由 (HomeController/FlowController) | 2 | ⬜ |
| 管理后台 (JobController/DocController) | 8+ | ⬜ |

> ✅ = 移动端/外部系统会调用,上位机需关注  
> ⬜ = 管理后台页面/内部调用,可忽略

---

## 三、接口详细清单

### 3.1 AppController — 统一网关入口

**基路径**: `/app`

| # | 路径 | 方法 | 说明 | appId携带 | 上位机关注 |
|---|------|------|------|-----------|:---:|
| 1 | `/app/{service:.+}` | POST | **核心入口**: 所有业务POST请求透传 | Header `APP_ID` | ✅ |
| 2 | `/app/{service:.+}` | GET | **核心入口**: 所有业务GET请求透传 | Header `APP_ID` | ✅ |
| 3 | `/app/{service:.+}` | PUT | 业务PUT请求透传 | Header `APP_ID` | ✅ |
| 4 | `/app/{service:.+}` | DELETE | 业务DELETE请求透传 | Header `APP_ID` | ✅ |
| 5 | `/app/{resource}/{action}` | GET | REST风格GET请求 | Header `APP_ID` | ✅ |
| 6 | `/app/{resource}/{action}` | POST | REST风格POST请求 | Header `APP_ID` | ✅ |
| 7 | `/app/ext/{serviceCode}/{appId}` | GET | 外部系统GET对接 | **URL路径** `{appId}` | ✅ |
| 8 | `/app/ext/{appId}/{serviceCode}` | POST | 外部系统POST对接 | **URL路径** `{appId}` | ✅ |

**说明**: 接口1-6通过 `super.initHeadParam()` 从Header提取 `APP_ID`，放入请求头Map传递给下游微服务。

---

### 3.2 OwnerController — 业主登录认证

**基路径**: `/app`

| # | 路径 | 方法 | 说明 | appId携带 | 社区标识 | 上位机关注 |
|---|------|------|------|-----------|----------|:---:|
| 9 | `/app/loginOwner` | POST | 业主微信登录 | Header `APP_ID` | appId | ✅ |
| 10 | `/app/loginOwnerByKey` | POST | 业主通过Key登录 | Header `APP_ID` | appId | ✅ |
| 11 | `/app/loginOwnerWechatAuth` | ANY | 微信公众号OAuth鉴权 | Query `appId` | appId | ✅ |
| 12 | `/app/refreshToken` | ANY | 刷新微信token | Header `app-id` | appId | ✅ |
| 13 | `/app/openServiceNotifyOpenId` | ANY | 公众号通知获取OpenId | Query `appId` | appId | ✅ |
| 14 | `/app/refreshOpenId` | ANY | 刷新OpenId(含wAppId) | Header `app-id` | appId | ✅ |
| 15 | `/app/getWId` | ANY | 获取微信WId | Query `appId` | appId | ✅ |
| 16 | `/app/loginStaffWechatAuth` | ANY | 员工微信OAuth鉴权 | Query `appId` | appId | ✅ |
| 17 | `/app/staffAuth` | ANY | 员工公众号授权 | Query `appId`+`communityId`+`storeId`+`staffId` | **双标识** | ✅ |
| 18 | `/app/loginWx` | POST | 微信小程序登录(code) | Header `APP_ID` | appId | ✅ |
| 19 | `/app/getWxPhoto` | POST | 获取微信手机号 | — (body传key) | ❌ 无 | ⚠️ |

**⚠️ 注意**: `getWxPhoto` 接口不携带appId/communityId，使用body中的key进行解密。

---

### 3.3 PropertyController — 物业员工登录

**基路径**: `/app`

| # | 路径 | 方法 | 说明 | appId携带 | 上位机关注 |
|---|------|------|------|-----------|:---:|
| 20 | `/app/loginProperty` | POST | 物业员工登录 | Header `APP_ID` | ✅ |

---

### 3.4 AppTokenController — Token校验

**基路径**: `/app/appToken`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 21 | `/app/appToken/checkToken` | ANY | Token有效性校验 | ⬜ (内部) |

---

### 3.5 EnvController — 环境查询

**基路径**: `/app/env`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 22 | `/app/env/getEnv` | GET | 获取当前环境(DEV/PROD) | ⬜ (简单查询) |

---

### 3.6 WechatGatewayController — 微信公众号网关 (被动接收)

**基路径**: `/app/wechat`

| # | 路径 | 方法 | 说明 | appId携带 | 上位机关注 |
|---|------|------|------|-----------|:---:|
| 23 | `/app/wechat/gateway/{appId}` | GET | 微信服务器URL验证(echostr) | **URL路径** | ✅ |
| 24 | `/app/wechat/gateway/{appId}` | POST | 微信消息/事件推送 | **URL路径** | ✅ |

**说明**: 这是微信服务器**主动推送**给我们的接口。上位机代理后，微信配置的回调URL要指向上位机地址，上位机根据 `{appId}` 路由到对应网关。

---

### 3.7 PaymentController — 支付下单 + 旧版回调

**基路径**: `/app/payment`

| # | 路径 | 方法 | 说明 | appId携带 | 上位机关注 |
|---|------|------|------|-----------|:---:|
| 25 | `/app/payment/toPay` | POST | 统一下单 | Header `APP_ID` | ✅ (下单本身走SDK直连,不走代理) |
| 26 | `/app/payment/toOweFeePay` | POST | 欠费支付下单 | Header `APP_ID` | ✅ |
| 27 | `/app/payment/toPayTempCarFee` | POST | 临时车缴费 | Header `APP_ID` | ✅ |
| 28 | `/app/payment/toPayTempCarInout` | POST | 临时车进出缴费 | Header `APP_ID` | ✅ |
| 29 | `/app/payment/rentingToPay` | POST | 出租缴费下单 | Header `APP_ID` | ✅ |
| 30 | `/app/payment/toQrOweFeePay` | POST | 二维码欠费支付 | Header `APP_ID` | ✅ |
| 31 | `/app/payment/toQrInGoOutPay` | POST | 二维码进出支付 | Header `APP_ID` | ✅ |
| 32 | `/app/payment/toQrBackCityPay` | POST | 二维码返城支付 | Header `APP_ID` | ✅ |
| 33 | `/app/payment/notify` | POST | 微信支付回调(旧版) | ❌ body中解析 | ⚠️ |
| 34 | `/app/payment/notifyChinaUms` | POST | 银联支付回调(旧版) | ❌ form参数 | ⚠️ |
| 35 | `/app/payment/oweFeeNotify` | POST | 欠费微信回调 | ❌ body中解析 | ⚠️ |
| 36 | `/app/payment/oweFeeNotifyChinaUms` | POST | 欠费银联回调 | ❌ form参数 | ⚠️ |
| 37 | `/app/payment/rentingNotify` | POST | 出租缴费回调 | ❌ body中解析 | ⚠️ |
| 38 | `/app/payment/tempCarFeeNotifyUrl` | POST | 临时车缴费回调 | ❌ body中解析 | ⚠️ |

**⚠️ 关键**: 旧版回调接口(33-38)**不携带appId在URL中**，需从body/form参数中解析出订单号后反查归属。

---

### 3.8 NotifyPaymentController — 通用支付回调 (新版)

**基路径**: `/app/payment/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 39 | `/app/payment/notify/common/{appId}/{communityId}` | POST | **通用回调** | ✅ **双标识**(URL) | ✅ |

**✅ 最佳实践**: 此接口URL中同时携带 `{appId}` 和 `{communityId}`，上位机可直接定位。

---

### 3.9 WechatNotifyPaymentController — 微信支付回调

**基路径**: `/app/payment/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 40 | `/app/payment/notify/wechat/{appId}/{paymentPoolId}` | POST | 微信支付回调 | ✅ `{appId}` (URL) | ✅ |
| 41 | `/app/payment/notify/nativeWechat/{appId}/{paymentPoolId}` | POST | 微信Native支付回调 | ✅ `{appId}` (URL) | ✅ |

---

### 3.10 AliPayNotifyController — 支付宝回调

**基路径**: `/app/alipay/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 42 | `/app/alipay/notify/receive` | POST | 支付宝支付回调 | ❌ 无(需从form中反查) | ⚠️ |

**⚠️ 上位机需特殊处理**: 支付宝回调URL不含appId，需从回调参数中的订单号/商户号反查归属网关。

---

### 3.11 ChangKNotifyPaymentController — 畅快聚合支付回调

**基路径**: `/app/payment/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 43 | `/app/payment/notify/changk/{appId}/{paymentPoolId}` | POST | 畅快支付回调 | ✅ `{appId}` (URL) | ✅ |
| 44 | `/app/payment/notify/nativechangk/{appId}/{paymentPoolId}` | POST | 畅快Native回调 | ✅ `{appId}` (URL) | ✅ |

---

### 3.12 ChinaUmsNotifyPaymentController — 银联商务回调

**基路径**: `/app/payment/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 45 | `/app/payment/notify/chinaums/{appId}/{communityId}` | POST | 银联商务回调 | ✅ **双标识**(URL) | ✅ |

---

### 3.13 RuiPayNotifyPaymentController — 瑞支付回调

**基路径**: `/app/payment/notify`

| # | 路径 | 方法 | 说明 | 全局标识 | 上位机关注 |
|---|------|------|------|----------|:---:|
| 46 | `/app/payment/notify/ruipay/{appId}/{paymentPoolId}` | POST | 瑞支付回调 | ✅ `{appId}` (URL) | ✅ |
| 47 | `/app/payment/notify/nativeruipay/{appId}/{paymentPoolId}` | POST | 瑞支付Native回调 | ✅ `{appId}` (URL) | ✅ |

---

### 3.14 GoodsController — 商品支付

**基路径**: `/goods`

| # | 路径 | 方法 | 说明 | appId携带 | 上位机关注 |
|---|------|------|------|-----------|:---:|
| 48 | `/goods/unifieldOrder` | POST | 商品统一下单 | Header `APP_ID` | ✅ (下单走SDK直连) |
| 49 | `/goods/notify` | POST | 商品支付回调 | ❌ body中解析 | ⚠️ |

---

### 3.15 ActivitiController — 工作流

**基路径**: `/app/activiti`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 50 | `/app/activiti/model/{modelId}/json` | GET | 获取工作流JSON | ⬜ (管理后台) |
| 51 | `/app/activiti/editor/stencilset` | GET | 获取编辑器模板 | ⬜ |
| 52 | `/app/activiti/model/{modelId}/save` | POST | 保存工作流模型 | ⬜ |

---

### 3.16 UserDownloadFileController — 文件下载

**基路径**: `/app/file/userfile`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 53 | `/app/file/userfile/download/{downloadId}/{token}` | GET | 用户文件下载 | ⬜ (认证token) |

---

### 3.17 CallComponentController — 组件调用 (内部)

**基路径**: `/callComponent`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 54 | `/callComponent/{api}` | ANY | 前台调用API组件 | ⬜ (内部) |
| 55 | `/callComponent/{componentCode}/{componentMethod}` | ANY | 调用指定组件方法 | ⬜ (内部) |
| 56 | `/callComponent/upload/{componentCode}/{componentMethod}` | POST | 组件文件上传 | ⬜ (内部) |
| 57 | `/callComponent/download/{componentCode}/{componentMethod}` | ANY | 组件文件下载 | ⬜ (内部) |

---

### 3.18 FlowController / HomeController — 页面路由

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 58 | `/` | GET | 首页(判断是否有商户信息) | ⬜ (管理后台) |
| 59 | `/flow/{flowCode}` | GET | 流程页面路由 | ⬜ (管理后台) |
| 60 | `/system/error` | GET | 错误页面 | ⬜ (管理后台) |

---

### 3.19 JobController — 定时任务管理

**基路径**: `/job`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 61 | `/job/` | GET | 任务管理首页 | ⬜ (管理后台) |
| 62 | `/job/queryFtpItems` | ANY | 查询FTP任务配置 | ⬜ |
| 63 | `/job/addFtpItem` | POST | 添加FTP任务 | ⬜ |
| 64 | `/job/questTaskTample` | ANY | 获取任务属性模板 | ⬜ |
| 65 | `/job/queryTaskAttrs` | ANY | 查询任务属性 | ⬜ |
| 66 | `/job/startJob` | ANY | 启动任务 | ⬜ |
| 67 | `/job/stopJob` | ANY | 停止任务 | ⬜ |
| 68 | `/job/deleteFtpItem` | ANY | 删除FTP任务 | ⬜ |
| 69 | `/job/editFtpItem` | POST | 编辑FTP任务 | ⬜ |

---

### 3.20 DocController — API文档

**基路径**: `/doc`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 70 | `/doc/api` | GET | API文档总览 | ⬜ (开发工具) |
| 71 | `/doc/api/page` | GET | 分页查询API | ⬜ |
| 72 | `/doc/api/{resource}` | GET | 按资源查API | ⬜ |
| 73 | `/doc/api/pageContent` | GET | 接口详细内容 | ⬜ |
| 74 | `/doc/api/{resource}/{serviceCode}` | GET | 指定服务API详情 | ⬜ |

---

### 3.21 出站代理接口 (新增)

**基路径**: `/proxy`

| # | 路径 | 方法 | 说明 | 上位机关注 |
|---|------|------|------|:---:|
| 75 | `/proxy/out` | ANY | **核心**: 客户机出站HTTP请求代理，通过 `__url__` 参数携带原始URL | ✅ 核心 |

**代理URL格式**: `{proxy-url}?__url__={url_encoded_original_url}`

**示例**:
```
原始请求: POST https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=xxx&secret=xxx
代理后:   POST http://host:8080/proxy/out?__url__=https%3A%2F%2Fapi.weixin.qq.com%2Fcgi-bin%2Ftoken%3Fgrant_type%3Dclient_credential%26appid%3Dxxx%26secret%3Dxxx
```

**上位机处理**: 从 `__url__` 参数解析原始URL，转发请求并返回响应。

---

## 四、回调接口汇总 (上位机重点关注)

### 4.1 按 appId 携带方式分类

#### ✅ 直接携带 appId (URL路径) — 上位机可直接路由

| # | URL | 渠道 | appId位置 |
|---|-----|------|-----------|
| 39 | `/app/payment/notify/common/{appId}/{communityId}` | 通用 | URL路径 |
| 40 | `/app/payment/notify/wechat/{appId}/{paymentPoolId}` | 微信 | URL路径 |
| 41 | `/app/payment/notify/nativeWechat/{appId}/{paymentPoolId}` | 微信Native | URL路径 |
| 43 | `/app/payment/notify/changk/{appId}/{paymentPoolId}` | 畅快 | URL路径 |
| 44 | `/app/payment/notify/nativechangk/{appId}/{paymentPoolId}` | 畅快Native | URL路径 |
| 45 | `/app/payment/notify/chinaums/{appId}/{communityId}` | 银联商务 | URL路径 |
| 46 | `/app/payment/notify/ruipay/{appId}/{paymentPoolId}` | 瑞支付 | URL路径 |
| 47 | `/app/payment/notify/nativeruipay/{appId}/{paymentPoolId}` | 瑞支付Native | URL路径 |
| 23 | `/app/wechat/gateway/{appId}` | 微信公众号 | URL路径 |

#### ⚠️ 不携带 appId — 上位机需从回调body反查

| # | URL | 渠道 | 反查方式 |
|---|-----|------|----------|
| 33 | `/app/payment/notify` | 微信支付(旧版) | 解析XML body中的 out_trade_no / appid |
| 42 | `/app/alipay/notify/receive` | 支付宝 | 解析form中的 out_trade_no |
| 34 | `/app/payment/notifyChinaUms` | 银联(旧版) | 解析form中的商户号 |
| 49 | `/goods/notify` | 商品支付 | 解析body中的订单号 |

**上位机处理策略**: 对于不携带appId的回调，需建立 `orderId/paymentPoolId → appId → sessionId` 的映射表（可直接查询网关已注册的 paymentPoolId 对应关系）。

---

## 五、出站代理业务覆盖范围

### 5.1 客户机自动拦截的出站请求

所有通过 `outRestTemplate` 发起的外部 HTTP 请求都会被客户机拦截并转发到上位机：

| 业务场景 | 原始URL示例 | 说明 |
|---------|------------|------|
| 微信API | `https://api.weixin.qq.com/cgi-bin/token` | access_token、用户信息等 |
| 微信支付 | `https://api.mch.weixin.qq.com/pay/unifiedorder` | 统一下单、查询、退款 |
| 银联商务 | `https://api-mop.chinaums.com/openapi/accessToken` | 支付、退款 |
| 第三方支付 | `https://api.pay.com/...` | 畅快支付、瑞支付、宝付等 |
| 短信发送 | `https://api.shlianlu.com/sms/...` | 联麓信息等 |
| 供应商接口 | 动态配置 | SCM供应商对接 |

### 5.2 客户机自动排除的出站请求

| 业务场景 | 排除机制 | 说明 |
|---------|---------|------|
| IOT物联网 | 动态配置 | 从 `MappingCache("IOT", "IOT_URL")` 读取 |
| OSS文件上传 | 动态配置 | 从 `MappingCache("OSS", "endpoint")` 读取 |
| FTP文件上传 | SDK直连 | 使用专用FTP客户端，不经过 outRestTemplate |
| 本地/内网调用 | 自动过滤 | 127.0.0.1、localhost、192.168.*、10.*、172.16-31.* |

**⚠️ 上位机注意**: IOT和OSS域名在客户机端动态配置，支持运行时修改。上位机无需处理这些请求的转发。

---

## 六、上位机集成关键结论

### 6.1 统一标识
- **appId** 是系统的全局路由标识，已覆盖 90% 以上的对外接口
- 所有移动端请求均通过 Header `APP_ID` 或 URL 路径携带 appId
- 上位机路由规则: `appId → communityId → sessionId(WebSocket连接)`

### 6.2 需要上位机代理的接口范围（最小集）

```
需要走代理的:
  ✅ 移动端全部 /app/* 请求        → 上位机转发 (INBOUND_REQUEST)
  ✅ 微信/支付宝等支付回调           → 上位机接收后转发 (INBOUND_REQUEST)
  ✅ 微信公众号消息推送              → 上位机接收后转发 (INBOUND_REQUEST)
  ✅ 客户机出站HTTP请求              → 上位机接收后转发 (HTTP代理)

不需要走代理的:
  ⬜ IOT物联网接口                  → 客户机直连
  ⬜ OSS文件上传                    → 客户机SDK直连
  ⬜ FTP文件上传                    → 客户机FTP直连
  ⬜ 管理后台页面(/flow, /job等)     → 不走上位机
  ⬜ 内部组件调用(/callComponent)    → 不走上位机
```

### 6.3 出站代理集成要点

1. **代理URL格式**: `{proxy-url}?__url__={url_encoded_original_url}`
2. **HTTP方法透传**: 上位机需保持原始请求的HTTP方法不变
3. **请求体透传**: 上位机需透传原始请求的body内容
4. **响应透传**: 上位机需透传原始响应的状态码、headers和body
5. **超时处理**: 建议超时时间与客户机保持一致（默认30秒）

### 6.4 上位机回调URL映射表设计

上位机 H2 数据库需维护:

```sql
-- 支付回调URL映射 (appId → paymentPoolId 互查)
CREATE TABLE callback_url_mapping (
    app_id VARCHAR(64) PRIMARY KEY,
    community_id VARCHAR(32),
    payment_pool_id VARCHAR(64),
    wechat_notify_url VARCHAR(256),
    alipay_notify_url VARCHAR(256),
    gateway_id VARCHAR(64),
    session_id VARCHAR(64)
);

-- 客户机注册信息 (用于出站代理路由)
CREATE TABLE client_register (
    client_key VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64),
    communities JSON,
    stores JSON,
    wechat_app_ids JSON,
    last_heartbeat TIMESTAMP
);
```

上位机接收到回调后:
1. 有 appId → 直接查 sessionId → CALLBACK_IN (WebSocket)
2. 无 appId → 从 body 提取 orderId → 查映射表 → 找到归属 gateway → CALLBACK_IN (WebSocket)