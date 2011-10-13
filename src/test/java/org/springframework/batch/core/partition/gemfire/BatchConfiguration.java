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
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfiguration {

    @Value("#{region}")
    private Region<String, StepExecution> region;

    @Autowired @Qualifier("jobRepository")
    private JobRepository jobRepository;

    @Value("classpath:/org/springframework/batch/core/schema-drop-h2.sql")
    private Resource h2Cleanup;

    @Value("classpath:/org/springframework/batch/core/schema-h2.sql")
    private Resource h2DatabasePopulatorResource;

    @Bean
    public static RemoteScope remoteScope() {
        RemoteScope rs = new RemoteScope();
        rs.setProxyTargetClass(true);
        return rs;
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
       dataSourceInitializer.setEnabled(false );
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


        String jdbcUrl = "jdbc:h2:tcp://localhost/~/batch-gemfire",
                jdbcPw = "",
                jdbcDriver = org.h2.Driver.class.getName(),
                jdbcUser = "sa";

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(jdbcUrl);
        basicDataSource.setPassword(jdbcPw);
        basicDataSource.setUsername(jdbcUser);
        basicDataSource.setDriverClassName(jdbcDriver);

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
    public Step step() throws Exception {
        SimpleStepFactoryBean<String, Object> bean = new SimpleStepFactoryBean<String, Object>();
        bean.setItemReader(new ExampleItemReader());
        bean.setItemWriter(new ExampleItemWriter());
        bean.setTransactionManager(this.transactionManager());
        bean.setJobRepository(this.jobRepository);
        return (TaskletStep)bean.getObject();
    }

    @Bean
    public PartitionHandler partitionHandler() throws Exception {
        GemfirePartitionHandler gemfirePartitionHandler = new GemfirePartitionHandler();
        gemfirePartitionHandler.setGridSize(2);
        gemfirePartitionHandler.setStep(  step());
        gemfirePartitionHandler.setRegion(this.region);
        return gemfirePartitionHandler;
    }

    @Bean
    public SimplePartitioner partitioner() {
        return new SimplePartitioner();
    }

}
