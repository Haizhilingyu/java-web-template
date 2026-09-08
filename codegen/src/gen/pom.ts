import type { Schema } from '../schema.js';

/** 生成 Maven pom：core 聚合 + model/repository/runtime/service 四个子模块 */
export function genPoms(schema: Schema): Record<string, string> {
    const p = schema.project;
    const groupId = p.groupId;
    const artifactId = p.artifactId;
    const parent = `    <parent>
        <groupId>${p.rootProject.groupId}</groupId>
        <artifactId>${p.rootProject.artifactId}</artifactId>
        <version>${p.rootProject.version}</version>
    </parent>`;

    const props = `    <properties>
        <maven.compiler.source>${p.javaVersion}</maven.compiler.source>
        <maven.compiler.target>${p.javaVersion}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>`;

    const jimmerStarter = `        <dependency>
            <groupId>org.babyfish.jimmer</groupId>
            <artifactId>jimmer-spring-boot-starter</artifactId>
            <version>\${jimmer.version}</version>
        </dependency>`;

    const aptExecutions = `        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <executions>
                    <!-- 根 pom 在这两个执行上配置了 lombok-only 的 processorPaths，
                         会遮蔽插件级配置，必须在相同执行 id 上覆盖为 jimmer-apt。
                         jimmer-apt 负责编译 src/main/dto 下的 DTO 文件 -->
                    <execution>
                        <id>default-compile</id>
                        <configuration>
                            <annotationProcessorPaths>
                                <path>
                                    <groupId>org.babyfish.jimmer</groupId>
                                    <artifactId>jimmer-apt</artifactId>
                                    <version>\${jimmer.version}</version>
                                </path>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                    <execution>
                        <id>default-testCompile</id>
                        <configuration>
                            <annotationProcessorPaths>
                                <path>
                                    <groupId>org.babyfish.jimmer</groupId>
                                    <artifactId>jimmer-apt</artifactId>
                                    <version>\${jimmer.version}</version>
                                </path>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>`;

    return {
        'pom.xml': `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
${parent}

    <groupId>${groupId}</groupId>
    <artifactId>${artifactId}</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>model</module>
        <module>repository</module>
        <module>runtime</module>
        <module>service</module>
    </modules>
${props}
</project>
`,
        'model/pom.xml': `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
${parent}

    <groupId>${groupId}</groupId>
    <artifactId>model</artifactId>
${props}
    <dependencies>
        <dependency>
            <groupId>org.babyfish.jimmer</groupId>
            <artifactId>jimmer-core</artifactId>
            <version>\${jimmer.version}</version>
        </dependency>
        <dependency>
            <groupId>org.babyfish.jimmer</groupId>
            <artifactId>jimmer-sql</artifactId>
            <version>\${jimmer.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${p.jacksonVersion}</version>
        </dependency>
    </dependencies>
    <build>${aptExecutions}
    </build>
</project>
`,
        'repository/pom.xml': `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
${parent}

    <groupId>${groupId}</groupId>
    <artifactId>repository</artifactId>
${props}
    <dependencies>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>model</artifactId>
            <version>\${project.version}</version>
        </dependency>
${jimmerStarter}
    </dependencies>
</project>
`,
        'runtime/pom.xml': `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
${parent}

    <groupId>${groupId}</groupId>
    <artifactId>runtime</artifactId>
${props}
    <dependencies>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>model</artifactId>
            <version>\${project.version}</version>
        </dependency>
        <!-- 过滤器/拦截器由 starter 自动发现并注册到 JSqlClient -->
${jimmerStarter}
    </dependencies>
</project>
`,
        'service/pom.xml': `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
${parent}

    <groupId>${groupId}</groupId>
    <artifactId>service</artifactId>
${props}
    <dependencies>
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>repository</artifactId>
            <version>\${project.version}</version>
        </dependency>
        <!-- 引入运行期基础设施(租户过滤/拦截器等) -->
        <dependency>
            <groupId>${groupId}</groupId>
            <artifactId>runtime</artifactId>
            <version>\${project.version}</version>
        </dependency>
${jimmerStarter}
        <!-- Input DTO 的 Bean Validation 校验(@Valid/@NotBlank 等) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
    <build>${aptExecutions}
    </build>
</project>
`,
    };
}
