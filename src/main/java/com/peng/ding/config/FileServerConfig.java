package com.peng.ding.config;

import com.itshidu.common.ftp.config.FtpPoolConfig;
import com.itshidu.common.ftp.core.FTPClientFactory;
import com.itshidu.common.ftp.core.FTPClientPool;
import com.itshidu.common.ftp.core.FtpClientUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@ConfigurationProperties(prefix = "ftp")
@Data
public class FileServerConfig {

    private String host;
    private String username;
    private String password;

    private int connectTimeOut;
    private int bufferSize;
    private int dataTimeout;


    private boolean passiveMode;
    private boolean blockWhenExhausted;
    private int maxWaitMillis;
    private int maxTotal;

    private int maxIdle;
    private int minIdle;
    private boolean testOnBorrow;
    private boolean testOnReturn;
    private boolean testOnCreate;
    private boolean testWhileIdle;

    @Bean
    public FtpClientUtils ftpUtils(){
        FtpPoolConfig config=new FtpPoolConfig();
        config.setHost(host);
        config.setPort(21);
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectTimeOut(connectTimeOut);
        config.setBufferSize(bufferSize);
        config.setDataTimeout(dataTimeout);
        config.setPassiveMode(passiveMode);
        config.setBlockWhenExhausted(blockWhenExhausted);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWaitMillis);
        config.setMaxTotal(maxTotal);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnCreate(testOnCreate);
        config.setTestOnReturn(testOnReturn);
        config.setTestWhileIdle(testWhileIdle);
        FTPClientFactory clientFactory=new FTPClientFactory(config); //对象工厂
        FTPClientPool pool = new FTPClientPool(clientFactory); //连接池对象
        FtpClientUtils util = new FtpClientUtils(pool); //工具对象
        return util;
    }
}
