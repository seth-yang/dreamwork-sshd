# dreamwork-sshd

### 介绍
使用 apache sshd 框架，提供一个可用的 sshd 服务器端半成品。快速开发基于 ssh 的 CLI应用程序。

### 软件架构
- 底层采用 `apache sshd` 框架来提供 `SSH` 网络服务。
- 内置一个实现了
  * RFC 318 (TELNET), 
  * RFC 854 – RFC 861 (TELNET sub-options)
  * RFC 1073 (TELNET Window Size)
  
  telnet协议栈的 java 实现
- 扩展 `org.dreamwork.telnet.command.Command` 以快速开发命令来实现功能

### 使用说明

#### 最简单的例子：
```java
import org.dreamwork.config.IConfiguration;
import org.dreamwork.config.PropertyConfiguration;
import org.dreamwork.network.sshd.Sshd;

import java.util.Properties;

public class MySshdApplication {
    public static void main (String[] args) throws Exception {
        IConfiguration conf = new PropertyConfiguration (new Properties ());

        Sshd sshd = new Sshd (conf);
        sshd.init (null);
        sshd.bind ();
    }
}
```
然后通过命令 
```shell script
ssh -p 50022 root@127.0.0.1
```
来登录服务，root的默认密码是`123456`。您可以在登录到sshd服务后使用`passwd`命令来修改root的密码

#### 配置 sshd 服务
- sshd 服务默认监听 `50022` 端口
- ca 文件的默认存储位置是 ${user.home}/.ssh-server/known-hosts
- sqlite 数据库文件的默认存储位置是 ${user.home}/.ssh-server/database.db

您可以通过配置来修改这些参数
- 键值 `service.sshd.port` 用来修改监听端口
- 键值 `service.sshd.cert.file` 用来修改 ca 文件的存放路径
- 键值 `database.file` 用来修改 sqlite 数据库文件的存放路径




### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request