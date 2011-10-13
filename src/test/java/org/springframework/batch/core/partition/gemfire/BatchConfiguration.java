package org.springframework.batch.core.partition.gemfire;


import com.gemstone.gemfire.cache.Region;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ImportResource("/META-INF/spring/theone.xml")
@PropertySource("/batch-h2.properties")
public class BatchConfiguration {

    @Autowired private Environment environment;

    @Autowired @Qualifier("region")
    private Region<String, StepExecution> region;

    @Autowired @Qualifier("jobRepository")
    private JobRepository jobRepository;

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

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(this.h2DatabasePopulatorResource);
        databasePopulator.setContinueOnError(true);

        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(this.dataSource());
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabaseCleaner(cleaner);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);

        return dataSourceInitializer;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(this.dataSource());
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

    @Bean
    public SimpleJobLauncher jobLauncher() {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(this.jobRepository);
        return jobLauncher;
    }

    @Bean
    @Scope("remote")
    public SimpleStepFactoryBean step() {
        SimpleStepFactoryBean<String, Object> bean = new SimpleStepFactoryBean<String, Object>();
        bean.setItemReader(new ExampleItemReader());
        bean.setItemWriter(new ExampleItemWriter());
        bean.setTransactionManager(this.transactionManager());
        bean.setJobRepository(this.jobRepository);
        return bean;
    }

    @Bean
    public PartitionHandler handler() throws Exception {
        GemfirePartitionHandler gemfirePartitionHandler = new GemfirePartitionHandler();
        gemfirePartitionHandler.setGridSize(2);
        gemfirePartitionHandler.setStep((Step) step().getObject());
        gemfirePartitionHandler.setRegion(this.region);
        return gemfirePartitionHandler;
    }

    @Bean
    public SimplePartitioner partitioner() {
        return new SimplePartitioner();
    }

}
