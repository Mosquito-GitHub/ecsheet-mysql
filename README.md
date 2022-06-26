# 基于Luckysheet实现的协同编辑在线表格

------

本项目由 https://gitee.com/DilemmaVi/ecsheet 分支而来，修复了几个BUG。
该项目基于国内最火的开源前端在线表格**Luckysheet** 可实现类似于 腾讯文档、金山云文档等在线Office工具的 在线表格功能。
此项目可以实现协同编辑，很利于在内网部署使用。
根据个人需求增加了导入文件，并实现sheet数据的拆分和合并，后续需要优化代码做到定制

本项目后端的语言是Java，主要技术栈如下：

> * 框架：SpringBoot + Websocket
> * 数据库：MySQL 8.0+
> * 前端核心：Luckysheet


### [友情链接：Luckysheet](https://github.com/mengshukeji/Luckysheet)
其文档：https://mengshukeji.gitee.io/LuckysheetDocs/zh/

> 🚀Luckysheet is an online spreadsheet like excel that is powerful, simple to configure, and completely open source.

------

## QuickStart

由于后端是基于Java和MySQL8的，需要提前安装相关环境

### 1. 安装MySQL8环境

安装MySQL 8.0

### 2. 编译项目jar包

mvn package
### 3. 修改配置像

修改application.yml

### 4. 访问地址
http://{你服务器的ip地址}:9999/index