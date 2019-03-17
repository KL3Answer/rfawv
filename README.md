# RFAWV
RFAWV == Reload Fields annotated with @Value(and injected by SpringBoot)。用于Spring Boot 配置文件热加载的工具，低开销、对代码零入侵。

### 1. EXAMPLE

simplest usage:

a. create a demo maven program and add dependency of RFAWV and spring boot starter:

    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>2.1.2.RELEASE</version>
    </dependency>
    <dependency>
        <groupId>com.github.kl3answer</groupId>
        <artifactId>reloadvalue-spring-boot-starter</artifactId>
        <version>VERSION</version>
    </dependency>
    
b. then create a standard springboot main class :

    package org.k3a.demo;
    
    import org.k3a.springboot.reloadvalue.annotation.EnableValueReload;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.boot.Banner;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    
    import java.util.List;
    import java.util.Map;
    
    /**
     * value-reload is triggered by adding  @EnableValueReload
     */
    @SpringBootApplication
    @EnableValueReload
    public class Bootstrapper {
        @Value("${my.secret}")
        private String secret;
    
        @Value("${size:111}")
        public int size;
    
        @Value("${${b}b}")
        public byte a;
    
        @Value("${ab}")
        public String ab;
    
        @Value("${test: #{T(java.lang.Integer).MAX_VALUE}}")
        private Integer test;
    
        @Value("#{'abc'.equals(\'${my.text}\')}")
        public String text;
    
        @Value("#{${smap}}")
        private Map<String, String> smap;
    
        @Value("${arr}")
        public List<String> arr1;
    
        /**
         *
         */
        public static void main(String[] args) {
            SpringApplication app = new SpringApplication(Bootstrapper.class);
            app.setBannerMode(Banner.Mode.OFF);
            app.setAddCommandLineProperties(true);
            app.run(args);
            System.out.println("starting...");
        }
    
        {
            // you can see these print lines will change when you change the config file like application.properties which is config
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(500);
                        System.out.println("=====" + secret + "====" + size + "======" + a+"==="+ab+"==="+test+"==="+text+"==="+smap+"==="+arr1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    
    }
    
 don't forget to create a properties file ,then named and place it in the conventional way of Spring,
in this example ,the content of config will be like:

    a=1
    b=a
    ab=5${a}
    size=222
    my.secret=${random.value}
    my.text=abd
    test=12
    smap={"a":"1","b":"2","c":"12"}
    arr=1,2,3,5
    c=2
    d1=135
    d2=33
    d3=4
    
c. start the demo and change the config file ,you will see the change in command line

d. if you want to be notified while config is changed, you can simply inject a bean :

    @Bean
    public Map<String, Consumer<String>> rvUpdateHandler() {
        final Map<String, Consumer<String>> consumers = new HashMap<>();
        // add some notify action , 
        // the key is the key in properties file,
        // the value is the notify method and the arg of which is the new value after reload
        //consumers.put("a",System.out::println);
        return consumers;
    }

### 2. Features

* support SpEL,and by using conversionService,it can support ALL types conversion
* less invasion

### 3. Dependencies
* JDK 1.8+
* Spring Boot 2.1.2 release
* slf4j 1.7.25
* jackson-databind 2.9.8

### 4. Unfinished
* @PropertySource annotation is not supported yet ,so it can not reload fields inject by @Value under intances of class annotated by @PropertySource
* the Spring framework should be work fine with this ,but as you can see ,it just do not support Spring framework right now ( since i haven't use Spring framework for a long time)
* the support of YAML and some other file format is left to finish
