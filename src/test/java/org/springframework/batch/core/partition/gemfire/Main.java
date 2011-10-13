package org.springframework.batch.core.partition.gemfire;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String []args) throws Exception {

//        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext ( BatchConfiguration.class) ;

        ClassPathXmlApplicationContext applicationContext  = new ClassPathXmlApplicationContext("/META-INF/spring/theone.xml");


    }
}
