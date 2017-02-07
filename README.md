# 配置指南

## 当前版本

    0.1.0-SNAPSHOT

## maven引用

	<dependency>
		<groupId>com.hrocloud.tiangong</groupId>
        <artifactId>filegw-api</artifactId>
        <version>0.1.0-SNAPSHOT</version>
	</dependency>

## Fastdfs

    目前已知会有两个FASTDFS集群，编号为0的是非受限访问集群，编号为1的是受限访问集群。

## 服务发布

### 编译部署方式

#### 编译、部署

	~ cd /home/admin/filegw
	~ ./deploy.sh
	~ cd service_deploy
	~ sh start.sh

#### 确认启动成功

	~ tail -f /home/admin/filegw/nohup.log
	~ tail -f /home/admin/logs/filegw/service.log

    如果启动成功，命令行输出将会提示：

	Server start OK in ? seconds.

    并且service.log中没有异常与错误信息。

### nginx/tengine

    确保域名 filegw.pajk.cn 指向该主机IP（或分发地址） ,当前请使用tiangong61:9709来代替
    在nginx/tengine配置文件中，将 filegw.pajk.cn 的请求以反向代理方式转发到 http://localhost:9700/
    确保关键URL的连通性：
        GET /file?token=xxx
        POST /upload?tfsGroupId=1
        POST /admin_upload?tfsGroupId=1
    有返回值，如200、30x、40x等均为正常。
    另有测试地址 GET /cookie 可以模拟生成cookie并测试TFS是否正常，是否开启对应配置项 spray.test.enabled

    配置示例

    server {

        listen 80;
        server_name filegw.pajk.cn;
        
        client_max_body_size 20M;

        location / {
            proxy_set_header Host $http_host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_pass http://127.0.0.1:9700;
        }

    }

### 客户端文件上传调用示例

    curl -X POST --verbose -F "file=@/Users/sean/Downloads/openjdk-small.png" -F "another=@openjdk-small-new.png" "http://filegw.pajk.cn/upload?tfsGroupId=1" --cookie "_tk=N%2F5Lwf9uzPsbpDysRU%2FVE7Bk6Qzq0%2Bc5N4zUlGl8pBo%3D"
    
