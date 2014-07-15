package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.executor.lsf.TestLsfErrorCheckerImpl;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestLsfStatusChecker {
    private int gpJobNo=0;
    private DrmJobRecord jobRecord;

    /*
     * Example bjobs -W output
     */    
    final String[] exampleOutput= {
            "JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME",
            "1583669 gpdev   RUN   genepattern gpint01     node1450    57940      07/09-11:39:51 default    033:23:02.00 6395   6910   1065,1066,1069,1070 07/09-11:39:52 -"
    };
    
    /*
     * More example output
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696268 gpdev   RUN   genepattern gpint01     node1457    66364      07/10-10:31:29 default    000:00:00.00 3      39     23412 07/10-10:31:31 - 
1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - 
1696270 gpdev   PEND  genepattern gpint01        -        66365      07/10-10:31:56 default    000:00:00.00 0      0       -  -  - 
-bash:gpint01:~ 1002 $ bjobs -W
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696268 gpdev   RUN   genepattern gpint01     node1457    66364      07/10-10:31:29 default    000:00:00.00 3      39     23412 07/10-10:31:31 - 
1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - 
-bash:gpint01:~ 1003 $ bjobs -W 1696270
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696270 gpdev   DONE  genepattern gpint01     node1456    66365      07/10-10:31:56 default    000:00:00.45 3      39     18696 07/10-10:32:00 07/10-10:32:01

     */
    
    @Before
    public void setUp() {
        jobRecord=new DrmJobRecord.Builder(gpJobNo)
        .build();
    }
    
    @Test
    public void parseCompletedJob() throws InterruptedException {
        /*
         Pending:
         1992893 gpdev   PEND  genepattern gpint01        -        66467      07/14-12:44:09 default    000:00:00.00 0      0       -  -  - 
         Finished:
         1992893 gpdev   DONE  genepattern gpint01     node1457    66467      07/14-12:44:09 default    000:00:00.14 3      39     23721 07/14-12:44:14 07/14-12:44:15
         */
        
        File lsfLogFile=FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, "completed_job.lsf.out");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1992893 gpdev   DONE  genepattern gpint01     node1457    66467      07/14-12:44:09 default    000:00:00.14 3      39     23721 07/14-12:44:14 07/14-12:44:15",
                lsfLogFile);
        assertEquals("drmJobId", ""+1992893, jobStatus.getDrmJobId());
        assertEquals("jobState", DrmJobState.DONE, jobStatus.getJobState());
        assertEquals("exitCode", (Integer)0, jobStatus.getExitCode());
        assertEquals("statusMessage", "Successfully completed.", jobStatus.getJobStatusMessage());
    }
    
    @Test
    public void parseRunningJob() throws InterruptedException {
        //expected date
        DateTime expectedSubmitTime=new DateTime("2014-07-09T11:39:51");
        DateTime expectedStartTime=new DateTime("2014-07-09T11:39:52");
        long expectedCpuUsage= (33L*60L*60L*1000L) + (23L*60*1000L) + (2L*1000L) +  570L;
        Memory expectedMemUsage=Memory.fromString("6395 mb");
        
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1583669 gpdev   RUN   genepattern gpint01     node1450    57940      07/09-11:39:51 default    033:23:02.57 6395   6910   1065,1066,1069,1070 07/09-11:39:52 - ");
        assertEquals("drmJobId", "1583669", jobStatus.getDrmJobId());
        assertEquals("jobState", DrmJobState.RUNNING, jobStatus.getJobState());
        assertEquals("submitTime", expectedSubmitTime.toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", expectedStartTime.toDate(), jobStatus.getStartTime());
        assertEquals("endTime", null, jobStatus.getEndTime());
        assertEquals("cpuUsage", expectedCpuUsage, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", expectedMemUsage, jobStatus.getMemory());
    }
    
    @Test
    public void parsePendingJob() throws InterruptedException  {
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - ");
        assertEquals("jobState", DrmJobState.QUEUED, jobStatus.getJobState());
        assertEquals("drmJobId", "1696266", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T10:31:01").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", null, jobStatus.getStartTime());
        assertEquals("endTime", null, jobStatus.getEndTime());
        assertEquals("cpuUsage", 0, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", 0, jobStatus.getMemory().getNumBytes());
    }
    
    @Test
    public void parseCancelledPendingJob() throws InterruptedException  {
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1696266 gpdev   EXIT  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  07/10-10:46:11");
        assertEquals("jobState", DrmJobState.ABORTED, jobStatus.getJobState());
        assertEquals("drmJobId", "1696266", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T10:31:01").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", null, jobStatus.getStartTime());
        assertEquals("endTime", new DateTime("2014-07-10T10:46:11").toDate(), jobStatus.getEndTime());
        assertEquals("cpuUsage", 0, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", 0, jobStatus.getMemory().getNumBytes());
    }
    
    @Test
    public void parseCancelledRunningJob() throws InterruptedException  {
        File lsfLogFile=FileUtil.getDataFile("jobResults/cancelledJob/.lsf.out");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1702585 gpdev   EXIT  genepattern gpint01     node1459    66373      07/10-12:31:23 default    000:00:02.40 32     2550   25751,25758,25760 07/10-12:31:24 07/10-12:32:15",
                lsfLogFile);
        assertEquals("jobState", DrmJobState.CANCELLED, jobStatus.getJobState());
        assertEquals("drmJobId", "1702585", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T12:31:23").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", new DateTime("2014-07-10T12:31:24").toDate(), jobStatus.getStartTime());
        assertEquals("endTime", new DateTime("2014-07-10T12:32:15").toDate(), jobStatus.getEndTime());
        assertEquals("cpuUsage", 2400, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", Memory.fromString("32mb"), jobStatus.getMemory());
    }
    
    /**
     * Special-case, when the job was terminated by the LSF system because the memory limit was reached.
     */
    @Test
    public void parseTerminatedMemLimit() throws InterruptedException {
        // load log file from LsfErrorCheckerImpl test case
        File lsfLogFile=FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, "memory_limit_lsf.out.txt");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "6284289 gpdev   EXIT  genepattern gpint01     node1459    23478      11/02-11:00:20 default    000:00:02.40 32     2550   25751,25758,25760 11/02-12:31:24 11/02-12:32:15",
                lsfLogFile);
        
        assertTrue("jobState for job cancelled by LSF because of memory limit", jobStatus.getJobState().is(DrmJobState.TERMINATED));
        assertTrue("Expecting RESOURCE_LIMIT jobState", jobStatus.getJobState().is(DrmJobState.RESOURCE_LIMIT));
        assertTrue("Expecting TERM_MEMLIMT jobState", jobStatus.getJobState().is(DrmJobState.TERM_MEMLIMIT));
    }
    
    /**
     * Special-case, when the job was terminated bty the LSF system because of a walltime limit.
     * @throws Exception
     */
    @Test
    public void parseTerminatedRunLimit() throws InterruptedException {
        File lsfLogFile=FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, "walltime_limit_lsf.out.txt");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1782219 gpdev   EXIT  genepattern gpint01     node1459    66406      07/11-12:44:43 default    000:00:03.29 33     2550   6625,6626,6628 07/11-12:44:45 07/11-12:46:00",
                lsfLogFile);

        assertTrue("jobState for job cancelled by LSF because of runlimit", jobStatus.getJobState().is(DrmJobState.TERMINATED));
        assertTrue("Expecting RESOURCE_LIMIT jobState", jobStatus.getJobState().is(DrmJobState.RESOURCE_LIMIT));
        assertTrue("Expecting TERM_MEMLIMT jobState", jobStatus.getJobState().is(DrmJobState.TERM_RUNLIMIT));
        assertEquals("expected exitCode", new Integer(134), jobStatus.getExitCode());
    }
    
    @Test
    public void parseNonZeroExitCode() throws InterruptedException {
        File lsfLogFile=FileUtil.getSourceFile(TestLsfErrorCheckerImpl.class, "non_zero_exit_code.lsf.out");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "2029936 gpdev   EXIT  genepattern gpint01     node1457    66472      07/14-20:24:39 default    000:00:00.43 3      39     10904 07/14-20:24:43 07/14-20:24:44",
                lsfLogFile);
        assertTrue("jobState isTerminated", jobStatus.getJobState().is(DrmJobState.TERMINATED));
        assertEquals("exitCode", (Integer)136, jobStatus.getExitCode());
        assertEquals("jobStatusMessage", "Exited with exit code 136.", jobStatus.getJobStatusMessage());
    }
    
    @Test
    public void checkStatus() throws Exception {
        File lsfLogFile=new File(".lsf.out");  // doesn't exist, should be ignored
        CmdRunner cmdRunner = new CmdRunner() {
            @Override
            public List<String> runCmd(List<String> cmd) throws CmdException {
                // skip the first line of output
                return Arrays.asList(new String[] { exampleOutput[1] });
            }
        };
        LsfStatusChecker statusChecker=new LsfStatusChecker(jobRecord, lsfLogFile, cmdRunner);
        statusChecker.checkStatus();
        DrmJobStatus jobStatus=statusChecker.getStatus();
        assertEquals(DrmJobState.RUNNING, jobStatus.getJobState());
    }
    
}
