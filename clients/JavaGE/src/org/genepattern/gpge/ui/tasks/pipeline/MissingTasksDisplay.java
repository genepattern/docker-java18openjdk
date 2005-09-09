package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.maindisplay.ViewManager;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.gpge.util.PostData;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

/**
 * Displays missing tasks in a pipeline
 * @author Joshua Gould
 *
 */
public class MissingTasksDisplay {

	private JPanel topPanel;
	private JScrollPane sp;


	public MissingTasksDisplay(final List missingTasks, final AnalysisService service) {

		final String[] columnNames = { "Name", "Version", "LSID" };
		TableModel tableModel = new AbstractTableModel() {

			public int getRowCount() {
				return missingTasks.size();
			}

			public int getColumnCount() {
				return columnNames.length;
			}

			public String getColumnName(int j) {
				return columnNames[j];
			}

			public Object getValueAt(int row, int col) {
				JobSubmission js = (JobSubmission) missingTasks.get(row);
				if (col == 0) {
					return js.getName();
				} else if (col == 1) {
					try {
						return new LSID(js.getLSID()).getVersion();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					return null;
				} else {
					return js.getLSID();
				}
			}
		};

		JTable table = new AlternatingColorTable(tableModel);

		JPanel buttonPanel = new JPanel();
		JButton installFromCatalogBtn = new JButton(
				"Install Missing Modules From Catalog");
		JButton refreshViewBtn = new JButton("Refresh View");
		refreshViewBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST, service));
			}

		});
		buttonPanel.add(installFromCatalogBtn);
		buttonPanel.add(refreshViewBtn);
		installFromCatalogBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				new Thread() {
					private boolean cancel = false;

					public void run() {
						JButton okButton = new JButton("OK");
						final JDialog dialog = new CenteredDialog(GenePattern.getDialogParent());
						final JLabel label = new JLabel("Preparing to install tasks...", JLabel.CENTER);
						
						try {
							dialog.getContentPane().add(label);
							JPanel buttonPanel = new JPanel();
							
							final JButton cancelButton = new JButton("Cancel");
							
							okButton.setEnabled(false);
							
							ActionListener btnListener = new ActionListener() {

								public void actionPerformed(ActionEvent e) {
									if(e.getSource()==cancelButton) {
										label.setText("Cancelling Installation...");
										cancel = true;
									} else {
										dialog.dispose();
									}
								}
								
							};
							cancelButton.addActionListener(btnListener);
							okButton.addActionListener(btnListener);
							
							buttonPanel.add(cancelButton);
							buttonPanel.add(okButton);
							
							dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
							TaskIntegratorProxy proxy = new TaskIntegratorProxy(
									AnalysisServiceManager.getInstance().getServer(),
									AnalysisServiceManager.getInstance().getUsername());
							dialog.setSize(300, 200);
							dialog.show();
							for (int i = 0; i < missingTasks.size(); i++) {
								JobSubmission js = (JobSubmission) missingTasks.get(i);
								label.setText("Installing " + js.getName() + "...");
								if(cancel) {
									break;
								}
								proxy.installTask(js.getLSID());
							}
						} catch (WebServiceException e1) {
							dialog.dispose();
							if(!GenePattern.disconnectedFromServer(e1)) {
								GenePattern.showErrorDialog("An error occurred while installing the missing modules.");
							}
							
						} finally {
							label.setText("Installation complete.");
							okButton.setEnabled(true);
						}
					}
				}.start();
				
			}

		});

		sp = new JScrollPane(table);

		JTextArea errorLabel = GUIUtil
				.createWrappedLabel("The following modules do not exist on the server. Please install the missing modules from the Module Repository or import them from a zip file (File > Import Module).");
		errorLabel.setBackground(Color.red);
		Border b = sp.getBorder();
		sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		topPanel = new JPanel(new BorderLayout());
		topPanel.add(errorLabel, BorderLayout.NORTH);
		topPanel.add(buttonPanel, BorderLayout.CENTER);
	}
	
	public JScrollPane getScrollPane() {
		return sp;
	}
	
	public JPanel getErrorPanel() {
		return topPanel;
	}
	

}
