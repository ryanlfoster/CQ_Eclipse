package cq_component.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import cq_component.util.ComponentConstants;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the
 * provided container. If the container resource (a folder or a project) is
 * selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "mpe". If a
 * sample multi-page editor (also available as a template) is registered for the
 * same extension, it will be able to open it.
 */

public class SampleNewWizard extends Wizard implements INewWizard {
	private SampleNewWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public SampleNewWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new SampleNewWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		final String componentName = page.getComponentName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, componentName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the file if missing
	 * or just replace its contents, and open the editor on the newly created
	 * file.
	 * @param componentName 
	 */

	private void doFinish(String containerName, String fileName,
			String componentName, IProgressMonitor monitor) throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName
					+ "\" does not exist.");
		}
		String folderName = fileName.toLowerCase();
		Boolean success = (new File(resource.getLocation().toString()+"\\"+folderName))
				.mkdirs();
		if (!success) {
		}else{
			try {
				
				File dotContentFile = new File(resource.getLocation().toString()+"\\"+folderName+"\\.content.xml");
				success = (dotContentFile).createNewFile();
				if(success){
					FileWriter fw = new FileWriter(dotContentFile);
					String text = createContentXmlText(fileName);
					fw.write(text);
					fw.flush();
				}
				
				File componentJsp = new File(resource.getLocation().toString()+"\\"+folderName+"\\"+folderName+".jsp");
				success = (componentJsp).createNewFile();
				if(success){
					FileWriter fw = new FileWriter(componentJsp);
					String text = "Change this out when you know what to put here!";
					fw.write(text);
					fw.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		resource = root.findMember(new Path(resource.getLocation().toString()+"\\"+fileName));
		
		
		
		IContainer container = (IContainer) resource;

		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream();
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	private String createContentXmlText(String fileName) {
		StringBuilder sb = new StringBuilder();
		sb.append(ComponentConstants.XML_ENCODING).append("\n");
		sb.append("<");
		sb.append(ComponentConstants.JCR_ROOT).append(" \n");
		sb.append(ComponentConstants.XML_NS).append(":jcr=\"").append(ComponentConstants.NS_JCR).append("\"\n");
		sb.append(ComponentConstants.XML_NS).append(":cq=\"").append(ComponentConstants.NS_CQ).append("\"\n");
		sb.append(ComponentConstants.XML_NS).append(":sling=\"").append(ComponentConstants.NS_SLING).append("\"\n");
		sb.append(ComponentConstants.JCR_PRIMARY_TYPE).append("=\"").append(ComponentConstants.CQ_COMPONENT).append("\"\n");
		sb.append(ComponentConstants.JCR_TITLE).append("=\"").append(fileName).append("\"\n");
		sb.append(ComponentConstants.JCR_DESCRIPTION).append("=\"").append(fileName).append(" ").append(ComponentConstants.COMPONENT).append("\"\n");
		sb.append(">\n");
		sb.append("<");
		sb.append(ComponentConstants.JCR_ROOT);
		sb.append("/>");
		return sb.toString();
	}

	/**
	 * We will initialize file contents with a sample text.
	 */

	private InputStream openContentStream() {
		String contents = "This is the initial file contents for *.mpe file that should be word-sorted in the Preview page of the multi-page editor";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "CQ_Component", IStatus.OK,
				message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}