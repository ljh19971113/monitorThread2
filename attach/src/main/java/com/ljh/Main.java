package com.ljh;

import net.bytebuddy.agent.VirtualMachine;
import sun.management.ConnectorAddressLink;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    /**
     * java -jar attach.jar {pid} {num} {time}
     * @param args
     */

    public static void main(String[] args){
        if (args.length < 3) {
            throw new RuntimeException("please check params");
        }
        VirtualMachine vm = null;
       try {
           vm = VirtualMachine.ForHotSpot.attach(args[0]);
           String curPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
           File parent = new File(curPath).getParentFile();
           String agentPath = parent.getPath() + File.separator + "threadAgent.jar";
           SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
           String outfilepath = parent.getPath() + File.separator + format.format(new Date()) + ".txt";
           vm.loadAgent(agentPath,args[1]+ ";" +args[2] + ";" + outfilepath);
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           try {
               vm.detach();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }
}
