<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.SuiteInfo,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 java.io.File,
		 java.net.MalformedURLException,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.ParseException,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Properties,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	String userID= (String)request.getAttribute("userID");
	LocalAdminClient adminClient = new LocalAdminClient("GenePattern");
try {
%>
	<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title>Installable Suites</title>
<script language="Javascript">
function checkSuite(frmName, bChecked) {
	frm = document.forms[frmName];
	bChecked = frm.checkit.checked;
	for (i = 0; i < frm.elements.length; i++) {
		if (frm.elements[i].type != "checkbox") continue;
		if (frm.elements[i].disabled == true) continue;
		frm.elements[i].checked = bChecked;
	}
}
</script>
</head><body>
<table width=100% cellspacing=0>
<tr><td colspan=3><jsp:include page="navbar.jsp"></jsp:include></td></tr>
<tr><td colspan=2>	
	<span id="fetching">
		Fetching suite catalog from <a href="<%= System.getProperty("SuiteRepositoryURL") %>" target="_new"><%= System.getProperty("SuiteRepositoryURL") %></a>...
	</span>

<%
	out.flush();	
SuiteRepository sr = null;
HashMap suites = new HashMap();
HashMap loadedSuites = new HashMap();
HashMap allSuites = new HashMap();
	try {
		sr = new SuiteRepository();
		suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));
		SuiteInfo[] loaded = adminClient.getLatestSuites();
		SuiteInfo[] allOfThem = adminClient.getAllSuites();
		for (int i=0; i < loaded.length; i++){
			loadedSuites.put(loaded[i].getLSID(), loaded[i]);
		}
		for (int i=0; i < allOfThem.length; i++){
			SuiteInfo si = allOfThem[i];
			LSID lsid = new LSID(si.getLSID());
			ArrayList verlist = (ArrayList)allSuites.get(lsid.toStringNoVersion());
			if (verlist == null){
				verlist = new ArrayList();
				allSuites.put(lsid.toStringNoVersion(), verlist);
			}
			verlist.add(si);
		}



	} catch (Exception e) {
%>
		Sorry, the <%=messages.get("ApplicationName")%> <a href="<%= System.getProperty("SuiteRepositoryURL") %>" target="_new">suite repository</a> is not currently available.<br>
		<p>Reason: <code><%= e.getMessage() %></code><br><p>
		<b>Try to correct this problem</b> by changing <a href="adminServer.jsp">web proxy settings</a> or <a href="adminServer.jsp">Suite Repository URL.</a>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
		e.printStackTrace();
		return;
	} finally {
		// erase the Fetching... message
%>
		<script language="Javascript">
			document.getElementById("fetching").innerHTML = "";
		</script>
<%
		out.flush();
	}
	String motd = sr.getMOTD_message();
	if (motd.length() > 0) {
%>
		<%= motd %><br>
		<font size="1">updated <%= DateFormat.getDateInstance().format(sr.getMOTD_timestamp()) %>.  
		<a href="<%= sr.getMOTD_url() %>" target="_blank">More information</a>.</font><br>
		<%= newerServerAvailable(sr.getMOTD_latestServerVersion(), System.getProperty("GenePatternVersion")) ? 
			("<a href=\"http://www.broad.mit.edu/cancer/software/genepattern/download\">Download updated "+messages.get("ApplicationName")+" version " + 
				sr.getMOTD_latestServerVersion() + "</a> (currently running version " + 
				System.getProperty("GenePatternVersion") + ")<br>") :
			 "" %>
		<br><hr>
<%
		for (int ii = 0; ii < 8*1024; ii++) out.print(" ");
		out.println();
		out.flush();
	}	
%>
<br></td></tr>
<tr><td align=center>

<form  action="editSuite.jsp" >
		<input type="submit" name="EditSuite" value="Create new Suite" />
&nbsp;
</form>

</td>
<td align=center>

<form  action="addZip.jsp" >
		<input type="submit" name="importSuite" value="Import Suite from zip" />
&nbsp;
</form>


</td></tr>
<tr><td colspan=2><hr></td></tr>
<tr><td colspan=2 align='center'><font size="+1"><b>New/Available Suites</b></font></td></tr><tr><td>&nbsp;</td></tr>
<%  
	for (Iterator iter = suites.keySet().iterator(); iter.hasNext(); ){
		HashMap suite = (HashMap)suites.get(iter.next());	
		
		LSID lsid = new LSID("a","n","1","1");
		try {
			lsid = new LSID((String)suite.get("lsid"));	
		} catch (Exception e) {}		
	
		boolean alreadyLoaded = ((loadedSuites.get(suite.get("lsid"))) != null);
		if (alreadyLoaded) continue;

		ArrayList modules = (ArrayList)suite.get("modules");
				boolean allInstalled = true;
		if (modules != null) {
		
			for (Iterator iter3 = modules.iterator(); iter3.hasNext(); ){
				HashMap mod = (HashMap)iter3.next();
				TaskInfo ti = adminClient.getTask((String)mod.get("lsid"));
				allInstalled  = allInstalled && (ti != null);
			}
		}
%>
<tr class='paleBackground'><td><font size=+1><b><%=suite.get("name")%></b></font>(<%=lsid.getVersion()%>)
<%
ArrayList docs = (ArrayList)suite.get("docFiles");
for (Iterator iter2 = docs .iterator(); iter2.hasNext(); ){

		String doc = (String)iter2.next();
		int idx = doc.lastIndexOf("/");
%>
		<a href='<%=doc%>'><img src="skin/pdf.jpg" border="0" alt="doc" align="top"></a>

<% }%>
</td> <td>
<table width=100%><tr>
<td>Author: <%=suite.get("author")%></td><td> Owner: <%=suite.get("owner")%><td>
</tr></table>
</td></tr>
<tr class='paleBackground'><td  colspan=2><%=suite.get("description")%></td></tr>

<tr class='paleBackground'>
<td valign='top' align='right'>

<form name="installSuite<%=suite.get("name")%>" action="installSuite.jsp" >
	<input type="hidden" name="suiteLsid" value="<%=suite.get("lsid")%>" >
	<input type="submit" name="InstallSuite" value="Install Suite" />&nbsp;
</form>

</td><td valign='top' align='left'>

<form name="install<%=suite.get("name")%>" action="taskCatalog.jsp" >
	<input type="hidden" name="checkAll" value="1" >
<%
	if (!allInstalled){
%>
	<input type="submit" name="install" value="install checked modules"/>
	<input type="checkbox" name="checkit" onClick="javascript:checkSuite('install<%=suite.get("name")%>', false)"/> Check all
<% } %>
</td>
</tr><tr>
<% 
int count = 0;
if (modules == null) modules = new ArrayList();
for (Iterator iter2 = modules.iterator(); iter2.hasNext(); ){
	HashMap mod = (HashMap)iter2.next();
	LSID modLsid = new LSID((String)mod.get("lsid"));
	String docName = (String)(mod.get("docFile"));

	int idx = -1;
	if (docName != null) {
		idx = docName.lastIndexOf("/");
	} else {
		
	}

	TaskInfo ti = adminClient.getTask(modLsid.toString());
	boolean installed = ti != null;
	
	if ( (count%2) == 0) out.println("<tr>");
	if (installed){
%>


<td>
<input type="checkbox" checked="true" name="LSID" disabled="true"/>
	<%=mod.get("name")%> (<%=modLsid.getVersion()%>) 
<%if (docName != null) { %>

<a href='<%= docName %>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> 
<%}
	String viewUrl = "addTask.jsp";
	if (installed){
		boolean isPipe = ti.getName().endsWith("pipeline");
		if (isPipe) viewUrl = "viewPipeline.jsp";
	}
%>
<a href="<%=viewUrl%>?view=1&name=<%=modLsid.toString()%>"><img src="skin/view.gif" alt="view" border="0" align="texttop"></a> 
</td>

<% 	} else { %>

<td>
<input type="checkbox" name="LSID" value="<%=modLsid.toString()%>"/>
	<%=mod.get("name")%> (<%=modLsid.getVersion()%>) 
<%if (docName != null) { %>
	<a href='<%= docName %>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> 
<%}%>
</td>

<% 	
	} 
	if ( (count%2) == 1) out.println("</tr>");
	count++; 
}
%>
</form>
<%
	}  // iterating over available suites
%>
</tr><tr><td colspan=2 align='center'><hr><font size="+1"><b>Loaded Suites</b></font></td></tr><tr><td>&nbsp;</td></tr>


<%  

	if (loadedSuites.size() == 0){
%>	
	 <tr><td colspan=2 align=center>No suites currently loaded</td></tr>
		
<%
	}	
	for (Iterator iter = loadedSuites.keySet().iterator(); iter.hasNext(); ){
		SuiteInfo suite = (SuiteInfo)loadedSuites.get(iter.next());	
		LSID lsid = new LSID(suite.getLSID());	
		String[] moduleLsids = suite.getModuleLSIDs();


		boolean allInstalled = true;
		for (int i=0; i < moduleLsids.length; i++){
			TaskInfo ti = adminClient.getTask(moduleLsids[i]);
			allInstalled  = allInstalled && (ti != null);
		}


%>
<tr class='altpaleBackground'>
<td>

<font size=+1><b><%=suite.getName()%></b></font>(<%=lsid.getVersion()%>)
<%

String[] docs = suite.getDocumentationFiles();
for (int k=0; k < docs.length; k++ ){
		String doc = docs[k];
%>
		<a href='getSuiteDoc.jsp?name=<%=suite.getLSID()%>&file=<%=doc%>'><img src="skin/pdf.jpg" border="0" alt="doc" align="top"></a>

<% }%>

</td><td>
<table width=100%><tr>
<td>Author: <%=suite.getAuthor()%></td><td> Owner: <%=suite.getOwner()%><td>
</tr></table>
</td>
</tr>
<tr class='altpaleBackground'>
<td  colspan=2><%=suite.getDescription()%></td>
</tr>

<tr class='altpaleBackground'>
<td valign='top' colspan=2>
<table width='100%' align='center'>
<tr>

<td>
<form name="deleteSuite<%=suite.getName()%>" action="deleteSuite.jsp" >
	<input type="submit" name="InstallSuite" value="Delete Suite verison" />
	<select name='suiteLsid'>
<%
	String lsidNoVer = (new LSID(suite.getLSID())).toStringNoVersion();
	ArrayList vers = (ArrayList)allSuites.get(lsidNoVer);
	for (Iterator iterV = vers.iterator(); iterV.hasNext(); ){
		SuiteInfo ver = (SuiteInfo)iterV.next();
		LSID vlsid = new LSID(ver.getLSID());
		String verStr = vlsid.getVersion();

		out.println("<option value='"+vlsid+"'>"+verStr);
	}
%>
	</select>

&nbsp;
</form>
</td>

<td >
<form name="editSuite<%=suite.getName()%>" action="editSuite.jsp" >
	<input type="hidden" name="suiteLsid" value="<%=suite.getLSID()%>" >
	<input type="submit" name="EditSuite" value="Edit Suite" />
&nbsp;
</form>
</td>
<td  >
<form name="zipSuite<%=suite.getName()%>" action="makeSuiteZip.jsp" >
	<input type="hidden" name="name" value="<%=suite.getLSID()%>" >
	<input type="submit" name="EditSuite" value="Export Suite" />
&nbsp;
</form>
</td>
<td valign='top' align='left'>
<form name="install<%=suite.getName()%>" action="taskCatalog.jsp" >
	<input type="hidden" name="checkAll" value="1" >
<%	if (!allInstalled) { %>
	<input type="submit" name="install" value="install checked modules"/>
	<input type="checkbox" name="checkit" onClick="javascript:checkSuite('install<%=suite.getName()%>', false)"/> Check all
<% } %>
</td>

</tr>
</table>

</td>
</tr>
<tr><% 
for (int i=0; i < moduleLsids.length; i++){
	//HashMap mod = (HashMap)iter2.next();
	
	LSID modLsid = new LSID(moduleLsids[i]);
	String docName = "fff"; // (String)(mod.get("docFile"));
	int idx = docName.lastIndexOf("/");

	TaskInfo ti = adminClient.getTask(modLsid.toString());
	boolean installed = ti != null;
	
	if ( (i%2) == 0) out.println("<tr>");

	String viewUrl = "addTask.jsp";
	if (installed){
		boolean isPipe = ti.getName().endsWith("pipeline");
		if (isPipe) viewUrl = "viewPipeline.jsp";
	
%>


<td>
<input type="checkbox" checked="true" name="LSID" disabled="true"/>
	<%=ti.getName()%> (<%=modLsid.getVersion()%>) 
<a href='getTaskDoc.jsp?name=<%=modLsid.toString()%>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> 
<a href="<%=viewUrl%>?view=1&name=<%=modLsid.toString()%>"><img src="skin/view.gif" alt="view" border="0" align="texttop"></a> 

</td>

<% 	} else { %>

<td>
<input type="checkbox" name="LSID" value="<%=modLsid.toString()%>"/>
	<%=ti.getName()%> (<%=modLsid.getVersion()%>) 
	<a href='<%= docName %>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> 
</td>

<% 	
	} 
	if ( (i%2) == 1) out.println("</tr>");
} 
%>

</form>

<%
		// end looping over loaded suites
	}
%>
</tr>
<tr><td colspan=2><jsp:include page="footer.jsp"></jsp:include></td> 
</tr></table>

<%
	} catch (Throwable t){
		t.printStackTrace();
	}
%>


<% // <jsp:include page="footer.jsp"></jsp:include> %>
</body>
</html>
<%! public boolean newerServerAvailable(String versionAtBroad, String localVersion) {
	boolean ret = false;
	StringTokenizer stBroadVersion = new StringTokenizer(versionAtBroad, LSID.VERSION_DELIMITER);
	StringTokenizer stLocalVersion = new StringTokenizer(localVersion, LSID.VERSION_DELIMITER);
	String broadVersionMinor;
	String localVersionMinor;
	int broadMinor;
	int localMinor;
	NumberFormat df = NumberFormat.getIntegerInstance();
	while (stBroadVersion.hasMoreTokens()) {
		broadVersionMinor = stBroadVersion.nextToken();
		if (!stLocalVersion.hasMoreTokens()) {
			// Broad version has more parts than local, but was equal until now
			// That means that it has an extra minor level and is therefore later
			ret = true;
			break;
		}
		localVersionMinor = stLocalVersion.nextToken();
		try {
			broadMinor = df.parse(broadVersionMinor).intValue();
		} catch (ParseException nfe) {
			// what to do?
			continue;
		}
		try {
			localMinor = df.parse(localVersionMinor).intValue();
		} catch (ParseException pe) {
			// what to do?
			continue;
		}
		if (broadMinor > localMinor) {
			ret = true;
			break;
		} else if (broadMinor < localMinor) {
			//System.out.println("You're running a greater version than downloadable");
			break;
		}
	}
	// don't worry about the case where the local version has more levels than the Broad version
	return ret;
    }
%>