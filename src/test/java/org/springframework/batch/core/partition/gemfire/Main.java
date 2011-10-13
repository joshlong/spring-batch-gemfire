package org.springframework.batch.core.partition.gemfire;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String []args) throws Exception {

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext ( BatchConfiguration.class) ;



    }
}
