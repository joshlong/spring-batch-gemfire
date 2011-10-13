package org.springframework.batch.core.partition.gemfire;


import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;

@Configuration
@ImportResource("/META-INF/spring/theone.xml")
@PropertySource("/batch-h2.properties")
public class BatchConfiguration {

    @Autowired private Environment environment;

    @Autowired @Qualifier("jobRepository") private JobRepository jobRepository;

    @Value("classpath:/org/springframework/batch/core/schema-drop-h2.sql")
    private Resource h2Cleanup;

    @Value("classpath:/org/springframework/batch/core/schema-h2.sql")
    private Resource h2DatabasePopulatorResource;

    private Log log = LogFactory.getLog(getClass());

    @Bean
    public static RemoteScope remoteScope() {
        return new RemoteScope();
    }

    @Bean
    public DataSourceInitializer initializer() throws Exception {

        ResourceDatabasePopulator cleaner = new ResourceDatabasePopulator();
        cleaner.addScript(this.h2Cleanup);
        cleaner.setIgnoreFailedDrops(true);
        cleaner.setContinueOnError(true);

        ResourceDatabasePopulator databasePopulator = new  ResourceDatabasePopulator();
        databasePopulator.addScript( this.h2DatabasePopulatorResource );
        databasePopulator.setContinueOnError(true);

        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(this.dataSource());
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabaseCleaner(cleaner);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);

        return dataSourceInitializer;
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        return new DataSourceTransactionManager(this.dataSource());
    }

    @Bean
    public SimpleJobLauncher jobLauncher() {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(this.jobRepository);
        return jobLauncher;
    }

    @Bean
    public DataSource dataSource() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(this.environment.getProperty("batch.jdbc.driver"));
        basicDataSource.setUrl(this.environment.getProperty("batch.jdbc.url"));
        basicDataSource.setUsername(this.environment.getProperty("batch.jdbc.user"));
        basicDataSource.setPassword(this.environment.getProperty("batch.jdbc.password"));
        return basicDataSource;
    }


}
