package com.ljh;

import com.ljh.entity.BusyThreadInfo;
import com.ljh.entity.ThreadSampler;
import com.ljh.entity.ThreadVO;
import com.ljh.utils.ArrayUtils;
import com.ljh.utils.ThreadUtil;
import sun.management.ConnectorAddressLink;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }


    private static synchronized void main(String args, final Instrumentation inst) {
        try {
            String[] argArr = args.split(";");
            for (int i=0;i<5;i++) {
                processTopBusyThreads(Integer.parseInt(argArr[0]), Long.parseLong(argArr[1]), argArr[2]);
                Thread.sleep(500);
            }
        } catch (Throwable exception) {
            //ignore
        }
    }
    /**
     * 参数三个  pid 采集数量 采集时长/ms
     * @throws IOException
     */
//    public static void main(String[] args) throws IOException {
//        if (args.length < 2) {
//            throw new RuntimeException("please check params");
//        }
//        processTopBusyThreads(Integer.parseInt(args[1]), Long.parseLong(args[2]));
//    }

    private static void processTopBusyThreads(int num, long sampleInterval, String filepath) throws IOException {
        ThreadSampler threadSampler = new ThreadSampler();
        threadSampler.sample(ThreadUtil.getThreads());
        threadSampler.pause(sampleInterval);
        List<ThreadVO> threadStats = threadSampler.sample(ThreadUtil.getThreads());

        int limit = Math.min(threadStats.size(), num);

        List<ThreadVO> topNThreads = null;
        if (limit > 0) {
            topNThreads = threadStats.subList(0, limit);
        } else { // -1 for all threads
            topNThreads = threadStats;
        }

        List<Long> tids = new ArrayList<Long>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            if (thread.getId() > 0) {
                tids.add(thread.getId());
            }
        }

        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(ArrayUtils.toPrimitive(tids.toArray(new Long[0])), false,
                false);
        if (tids.size()> 0 && threadInfos == null) {
            return;
        }

        //threadInfo with cpuUsage
        List<BusyThreadInfo> busyThreadInfos = new ArrayList<BusyThreadInfo>(topNThreads.size());
        for (ThreadVO thread : topNThreads) {
            ThreadInfo threadInfo = findThreadInfoById(threadInfos, thread.getId());
            if (threadInfo != null) {
                BusyThreadInfo busyThread = new BusyThreadInfo(thread, threadInfo);
                busyThreadInfos.add(busyThread);
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filepath,true));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = dateFormat.format(new Date());
        writer.write(date + "\n");

        for (BusyThreadInfo info : busyThreadInfos) {
            String stacktrace = ThreadUtil.getFullStacktrace(info, -1, -1);
            writer.write(stacktrace);
        }

        writer.close();
    }

    private static ThreadInfo findThreadInfoById(ThreadInfo[] threadInfos, long id) {
        for (int i = 0; i < threadInfos.length; i++) {
            ThreadInfo threadInfo = threadInfos[i];
            if (threadInfo != null && threadInfo.getThreadId() == id) {
                return threadInfo;
            }
        }
        return null;
    }
}
