# MiniClaw AI Assistant Framework

MiniClaw 是一个轻量级、模块化、无重型中间件依赖（无 Spring Boot）的 Java AI 助手核心框架。
本项目旨在提供高度可扩展的 LLM 抽象、企业级插件工具系统，以及具备深度思考和任务拆解能力的任务调度引擎。

## 🚀 核心功能 (Core Features)

### 1. 多 LLM 提供商的统一抽象
- 定义了标准化的 `LLMProvider` 接口，抹平了不同大模型 API 之间的结构差异。
- 原生支持 **Function Calling（工具调用）** 与 **SSE（流式输出）**。
- 支持无缝切换大模型提供商（已内置 DeepSeek 与 Ollama 的适配实现），只需修改 `application.yml` 配置文件即可实现零代码切换，并支持异常降级（Mock Fallback）机制。

### 2. 基于插件架构的工具系统
- 采用企业级“微内核”设计，核心框架（Host）与具体工具（Plugin）在物理文件上完全解耦。
- 工具开发者只需实现框架的 `Tool` 接口，提供工具名称、描述、JSON Schema（参数定义）以及执行逻辑，并打包为独立的 `.jar` 文件即可。

### 3. 动态工具发现机制
- 抛弃了静态硬编码注册的模式。框架内置了基于 `URLClassLoader` 的 `PluginLoader`。
- 程序启动时，会自动扫描根目录下的 `plugins/` 文件夹。
- 动态加载外部 `.jar` 包，提取其中的 `.class` 字节码文件，利用 Java 反射自动实例化并注册到 `ToolRegistry`，实现**“约定优于配置”**的即插即用体验。

### 4. 智能任务管理系统 (Task Management)
- **异步调度**：基于 `ExecutorService` 实现请求非阻塞处理，具有完整的任务生命周期状态管理（PENDING, RUNNING, SUCCESS, FAILED）。
- **任务拆解 (Planning Phase)**：面对用户的复杂请求，系统会引导大模型首先进行“思考”，将复杂需求拆解为分步逻辑执行计划。
- **ReAct 自主循环 (Reasoning and Acting)**：打破了固定轮数的限制。大模型可以自主决定连续调用多个工具（Chain of Tools）。工具的执行结果或报错异常（Exception）会作为观察结果（Observation）实时喂回给大模型，赋予系统**反思与自我纠错**的能力。

---

## 🛠️ 快速开始 (Quick Start)

### 环境要求
- Java 17 或更高版本
- Maven 3.8+

### 1. 配置 API Key
打开 `src/main/resources/application.yml`，填入您的模型配置信息：
```yaml
llm:
  provider: deepseek
  deepseek:
    apiKey: "YOUR_API_KEY_HERE"
    url: "https://api.deepseek.com/chat/completions"
    model: "deepseek-chat"
```

### 2. 编译与运行
在项目根目录下执行以下命令：
```bash
# 编译项目
mvn clean compile

# 运行主程序
mvn exec:java
```

### 3. 如何开发并安装新插件？
1. 创建一个普通的 Java 类，实现 `com.miniclaw.tool.Tool` 接口。
2. 将其编译为 `.class` 文件并打包成 `.jar`（例如 `my-tool.jar`）。
3. 将 `my-tool.jar` 放入项目根目录的 `plugins/` 文件夹下。
4. 重启 MiniClaw 框架，控制台将提示自动发现并挂载了您的新工具。您便可以直接用自然语言要求大模型调用它。

---

## 🏗️ 目录结构 (Architecture)

```text
MCLAW/
 ├── src/main/java/com/miniclaw/
 │    ├── config/        # YAML 配置读取
 │    ├── context/       # 会话历史与上下文管理
 │    ├── llm/           # 大模型 Provider 抽象与实现
 │    ├── task/          # 异步任务管理与 ReAct 调度引擎
 │    ├── tool/          # 插件接口与类加载器扫描机制
 │    └── Main.java      # 框架启动入口与交互控制台
 ├── src/main/resources/
 │    └── application.yml# 全局配置文件
 ├── plugins/            # [动态发现目录] 存放第三方编译好的 .jar 插件
 └── pom.xml             # 依赖管理
```
