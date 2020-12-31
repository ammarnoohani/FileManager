package FileManager;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileSystemView;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;

import java.io.*;
import java.nio.channels.FileChannel;

import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


public class FileManager {


    public static final String APP_TITLE = "FileManager";
    private Desktop desktop;
    private FileSystemView fileSystemView;

    private File[] filesForTest;

    private File currentFile;
    private JPanel gui;

    private JTree tree;
    private DefaultTreeModel treeModel;

    private JTable table;
    private JProgressBar progressBar;
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;

    private JButton openFile;
    private JButton printFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton copyFile;
    private JButton seprateFile;
    private JButton findDuplicates;
    private JButton deleteDuplicates;

    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    public Container getFrame() {
        if (gui==null) {
            gui = new JPanel(new BorderLayout(3,3));
            gui.setBorder(new EmptyBorder(5,5,5,5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3,3));
            //fileTableModel = new FileTableModel();

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails( ((FileTableModel)table.getModel()).getFile(row) );
                }
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(new Dimension((int)d.getWidth(), (int)d.getHeight()/2));
            detailView.add(tableScroll, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse){
                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode)tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File)node.getUserObject());
                }
            };

            // show the file system roots.
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add( node );
                //showChildren(node);
                //
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
                //
            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            // as per trashgod tip
            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(
                    200,
                    (int)preferredSize.getHeight());
            treeScroll.setPreferredSize( widePreferred );

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4,2));
            fileMainDetails.setBorder(new EmptyBorder(0,6,0,6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0,1,2,2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING,4,0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);

            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);

            int count = fileDetailsLabels.getComponentCount();
            for (int ii=0; ii<count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            // mnemonics stop working in a floated toolbar
            toolBar.setFloatable(false);

            openFile = new JButton("Open");
            openFile.setMnemonic('o');

            openFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.open(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                    gui.repaint();
                }
            });
            toolBar.add(openFile);
            toolBar.addSeparator();
            toolBar.addSeparator();

            editFile = new JButton("Edit");
            editFile.setMnemonic('e');
            editFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.edit(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                }
            });
//            toolBar.add(editFile);

            printFile = new JButton("Print");
            printFile.setMnemonic('p');
            printFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    try {
                        desktop.print(currentFile);
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                }
            });
//            toolBar.add(printFile);

            // Check the actions are supported on this platform!
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));
            printFile.setEnabled(desktop.isSupported(Desktop.Action.PRINT));



            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    newFile();
                }
            });
            toolBar.add(newFile);
            toolBar.addSeparator();
            toolBar.addSeparator();

            copyFile = new JButton("Copy");
            copyFile.setMnemonic('c');
            copyFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    showErrorMessage("'Copy' not implemented.", "Not implemented.");
                }
            });
//            toolBar.add(copyFile);

            JButton renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    renameFile();
                }
            });
            toolBar.add(renameFile);
            toolBar.addSeparator();
            toolBar.addSeparator();

            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    deleteFile();
                }
            });
            toolBar.add(deleteFile);
            toolBar.addSeparator();
            toolBar.addSeparator();

            findDuplicates = new JButton("Find Duplicates");
            findDuplicates.setMnemonic('f');
            findDuplicates.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    FindDuplicates();
                }
            });
            toolBar.add(findDuplicates);
            toolBar.addSeparator();
            toolBar.addSeparator();


            deleteDuplicates = new JButton("Delete All Duplicates");
            deleteDuplicates.setMnemonic('d');
            deleteDuplicates.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    deleteAllDuplicates();
                    JOptionPane.showMessageDialog(null,"Successfully Deleted All Duplicates Files.","Alert",JOptionPane.WARNING_MESSAGE);
                }
            });
            toolBar.add(deleteDuplicates);
            toolBar.addSeparator();
            toolBar.addSeparator();

            seprateFile = new JButton("Seprate Fles");
            seprateFile.setMnemonic('s');
            seprateFile.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    int input = JOptionPane.showConfirmDialog(null, "Do you want to seprate files");
                    if(input==0){
                        seprateFiles();
                        JOptionPane.showMessageDialog(null,"Successfully Seprated. Thanks!","Alert",JOptionPane.WARNING_MESSAGE);
                    }
                    else if(input==2){
                        JOptionPane.showMessageDialog(null,"Canceled","Alert",JOptionPane.WARNING_MESSAGE);

                    }


                }
            });
            toolBar.add(seprateFile);
            toolBar.addSeparator();
            toolBar.addSeparator();

            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            //readable.setEnabled(false);
//            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            //writable.setEnabled(false);
//            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            //executable.setEnabled(false);
//            toolBar.add(executable);

            JPanel fileView = new JPanel(new BorderLayout(3,3));

            fileView.add(toolBar,BorderLayout.NORTH);
            fileView.add(fileMainDetails,BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    treeScroll,
                    detailView);
            gui.add(splitPane, BorderLayout.CENTER);

            JPanel simpleOutput = new JPanel(new BorderLayout(3,3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(simpleOutput, BorderLayout.SOUTH);

        }
        return gui;
    }

    private class DuplicateFilesTable extends  JFrame{

        JTable DFtable ;
        File[] dupFiles = setDupFiles(filesForTest);
        String cpNameWini , cpNameLinuxi, cpNameWinj , cpNameLinuxj;
        File delDupFile;
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel delPanel = new JPanel(new BorderLayout());
        JButton delButtton = new JButton("Delete File");
        JLabel delLabel = new JLabel("Select File to Delete! ");



        private String FileCreationTime(File file){
            BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                FileTime time = attrs.creationTime();

                String pattern = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

                String formatted = simpleDateFormat.format( new Date( time.toMillis() ) );
                return formatted;
            } catch (IOException e) {

                e.printStackTrace();
            }
            return null;

        }
        private String FileExtension(File file){
            return FilenameUtils.getExtension(file.getName());
        }
        private String ExtensionRemover(File file){
            String str = file.getName();
            String str2 = "";
            for(int  i=0; i<str.length();i++){
                if(str.charAt(i)=='.'){
                    break;
                }
                else{
                    str2 += str.charAt(i);
                }
            }
            return str2;
        }

        private int getDupFilesLength(File[] filesForTest){
            int c= 0;
            for(int i = 0; i < filesForTest.length-1; i++) {
                for(int j = i + 1; j < filesForTest.length; j++) {
                    cpNameLinuxi = ExtensionRemover(filesForTest[i])+ " -(copy)."+FileExtension(filesForTest[i]);
                    cpNameWini = ExtensionRemover(filesForTest[i])+ " -Copy."+FileExtension(filesForTest[i]);
                    cpNameLinuxj = ExtensionRemover(filesForTest[j])+ " -(copy)."+FileExtension(filesForTest[i]);
                    cpNameWinj = ExtensionRemover(filesForTest[j])+ " -Copy."+FileExtension(filesForTest[i]);

                    if(filesForTest[i].isDirectory()){}
                    else if((filesForTest[i].length()==filesForTest[j].length() &&
                            FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) )
                            ||
                            (FileCreationTime(filesForTest[i]).equals(FileCreationTime(filesForTest[j])) &&
                                    FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) &&
                                    filesForTest[i].length()==filesForTest[j].length()
                            )){
                        c++;c++;
                    }
                    else if(cpNameWini.equals(filesForTest[j].getName()) || cpNameLinuxi.equals(filesForTest[j].getName()) ){
                        c++;c++;
                    }
                    else if(cpNameWinj.equals(filesForTest[i].getName()) || cpNameLinuxj.equals(filesForTest[i].getName()) ){
                        c++;c++;
                    }


                }
            }
            gui.repaint();
            return c;

        }
        private File[] setDupFiles(File[] files){
            File[] dupFiles = new File[getDupFilesLength(filesForTest)];
            int c=0;
            for(int i = 0; i < filesForTest.length-1; i++) {
                for(int j = i + 1; j < filesForTest.length; j++) {
                    cpNameLinuxi = ExtensionRemover(filesForTest[i])+ " -(copy)."+FileExtension(filesForTest[i]);
                    cpNameWini = ExtensionRemover(filesForTest[i])+ " -Copy."+FileExtension(filesForTest[i]);
                    cpNameLinuxj = ExtensionRemover(filesForTest[j])+ " -(copy)."+FileExtension(filesForTest[i]);
                    cpNameWinj = ExtensionRemover(filesForTest[j])+ " -Copy."+FileExtension(filesForTest[i]);

                    if(filesForTest[i].isDirectory()){}
                    else if((filesForTest[i].length()==filesForTest[j].length() &&
                            FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) )
                            ||
                            (FileCreationTime(filesForTest[i]).equals(FileCreationTime(filesForTest[j])) &&
                                    FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) &&
                                    filesForTest[i].length()==filesForTest[j].length()
                            )){
                        dupFiles[c++]=filesForTest[i];
                        dupFiles[c++]=filesForTest[j];
                    }
                    else if(cpNameWini.equals(filesForTest[j].getName()) || cpNameLinuxi.equals(filesForTest[j].getName()) ){
                        dupFiles[c++]=filesForTest[i];
                        dupFiles[c++]=filesForTest[j];
                    }
                    else if(cpNameWinj.equals(filesForTest[i].getName()) || cpNameLinuxj.equals(filesForTest[i].getName()) ){
                        dupFiles[c++]=filesForTest[i];
                        dupFiles[c++]=filesForTest[j];
                    }


                }
            }
            gui.repaint();
            return dupFiles;

        }

        private DuplicateFilesTable(){



            setLayout(new FlowLayout());

            String [] columns = {"Name" ,"Path", "Size","Created Time", "Last Modified"};
            Object[][] filesObj = new Object[dupFiles.length][columns.length];


            for(int i=0 ; i<dupFiles.length ;i++ ){
                for (int j=0; j<columns.length;j++){
                    if(j==0)
                        filesObj[i][j]=dupFiles[i].getName();
                    if(j==1)
                        filesObj[i][j]=dupFiles[i].getAbsolutePath();
                    if(j==2)
                        filesObj[i][j]=dupFiles[i].length();
                    if(j==3)
                        filesObj[i][j]=(FileCreationTime(dupFiles[i]));
                    if(j==4)
                        filesObj[i][j]=(new Date(dupFiles[i].lastModified()).toString());
                }
            }


            DFtable = new JTable(filesObj,columns);
            DFtable.setPreferredScrollableViewportSize(new Dimension(1000,500));
            DFtable.setFillsViewportHeight(true);
            DFtable.setRowHeight(30);
            DFtable.setFont(new Font(Font.DIALOG,  Font.BOLD, 13));
            DFtable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
                public void valueChanged(ListSelectionEvent event) {
                    delDupFile = new File(DFtable.getValueAt(DFtable.getSelectedRow(), 1).toString());
                }
            });

            delButtton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
					/*int input = JOptionPane.showConfirmDialog(null, "Deleted File will not be restored! Are You Sure?");
					 *                    if(input==0){
					 *                        deleteDuplicateFile(delDupFile);
					 *                        JOptionPane.showMessageDialog(null,"Successfully Deleted.","Alert",JOptionPane.WARNING_MESSAGE);
					 }
					 else if(input==2){
						 JOptionPane.showMessageDialog(null,"Canceled","Alert",JOptionPane.WARNING_MESSAGE);

					 }*/
                    deleteFile(delDupFile);
                    JOptionPane.showMessageDialog(null,"Successfully Deleted !.","Alert",JOptionPane.WARNING_MESSAGE);

                    setVisible(false);
                    FindDuplicates();
                }
            });

            JScrollPane scrollPane = new JScrollPane(DFtable);

            delPanel.add(delButtton, BorderLayout.LINE_END);
            delPanel.add(delLabel, BorderLayout.CENTER);
            mainPanel.add(scrollPane , BorderLayout.PAGE_START);
            mainPanel.add(delPanel , BorderLayout.PAGE_END);
            add(mainPanel);


        }

    }
    private void FindDuplicates(){
        DuplicateFilesTable dupTable = new DuplicateFilesTable();
        dupTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dupTable.pack();
        dupTable.setLocationByPlatform(true);
        dupTable.setSize(1200 , 600);
        dupTable.setVisible(true);
        dupTable.setTitle("Duplicate Files");

    }
    private int getDupFilesLength(File[] filesForTest){
        String cpNameWini , cpNameLinuxi, cpNameWinj , cpNameLinuxj;

        int c= 0;
        for(int i = 0; i < filesForTest.length-1; i++) {
            for(int j = i + 1; j < filesForTest.length; j++) {
                cpNameLinuxi = ExtensionRemover(filesForTest[i])+ " -(copy)."+FileExtension(filesForTest[i]);
                cpNameWini = ExtensionRemover(filesForTest[i])+ " -Copy."+FileExtension(filesForTest[i]);
                cpNameLinuxj = ExtensionRemover(filesForTest[j])+ " -(copy)."+FileExtension(filesForTest[i]);
                cpNameWinj = ExtensionRemover(filesForTest[j])+ " -Copy."+FileExtension(filesForTest[i]);

                if(filesForTest[i].isDirectory()){}
                else if((filesForTest[i].length()==filesForTest[j].length() &&
                        FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) )
                        ||
                        (FileCreationTime(filesForTest[i]).equals(FileCreationTime(filesForTest[j])) &&
                                FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) &&
                                filesForTest[i].length()==filesForTest[j].length()
                        )){
                    c++;c++;
                }
                else if(cpNameWini.equals(filesForTest[j].getName()) || cpNameLinuxi.equals(filesForTest[j].getName()) ){
                    c++;c++;
                }
                else if(cpNameWinj.equals(filesForTest[i].getName()) || cpNameLinuxj.equals(filesForTest[i].getName()) ){
                    c++;c++;
                }


            }
        }
        gui.repaint();
        return c;

    }
    private File[] setDupFiles(File[] files){
        String cpNameWini , cpNameLinuxi, cpNameWinj , cpNameLinuxj;

        File[] dupFiles = new File[getDupFilesLength(filesForTest)];
        int c=0;
        for(int i = 0; i < filesForTest.length-1; i++) {
            for(int j = i + 1; j < filesForTest.length; j++) {
                cpNameLinuxi = ExtensionRemover(filesForTest[i])+ " -(copy)."+FileExtension(filesForTest[i]);
                cpNameWini = ExtensionRemover(filesForTest[i])+ " -Copy."+FileExtension(filesForTest[i]);
                cpNameLinuxj = ExtensionRemover(filesForTest[j])+ " -(copy)."+FileExtension(filesForTest[i]);
                cpNameWinj = ExtensionRemover(filesForTest[j])+ " -Copy."+FileExtension(filesForTest[i]);

                if(filesForTest[i].isDirectory()){}
                else if((filesForTest[i].length()==filesForTest[j].length() &&
                        FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) )
                        ||
                        (FileCreationTime(filesForTest[i]).equals(FileCreationTime(filesForTest[j])) &&
                                FileExtension(filesForTest[i]).equals(FileExtension(filesForTest[j]) ) &&
                                filesForTest[i].length()==filesForTest[j].length()
                        )){
                    dupFiles[c++]=filesForTest[i];
                    dupFiles[c++]=filesForTest[j];
                }
                else if(cpNameWini.equals(filesForTest[j].getName()) || cpNameLinuxi.equals(filesForTest[j].getName()) ){
                    dupFiles[c++]=filesForTest[i];
                    dupFiles[c++]=filesForTest[j];
                }
                else if(cpNameWinj.equals(filesForTest[i].getName()) || cpNameLinuxj.equals(filesForTest[i].getName()) ){
                    dupFiles[c++]=filesForTest[i];
                    dupFiles[c++]=filesForTest[j];
                }


            }
        }
        gui.repaint();
        return dupFiles;

    }
    private String FileCreationTime(File file){
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime time = attrs.creationTime();

            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            String formatted = simpleDateFormat.format( new Date( time.toMillis() ) );
            return formatted;
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;

    }
    private String FileExtension(File file){
        return FilenameUtils.getExtension(file.getName());
    }
    private String ExtensionRemover(File file){
        String str = file.getName();
        String str2 = "";
        for(int  i=0; i<str.length();i++){
            if(str.charAt(i)=='.'){
                break;
            }
            else{
                str2 += str.charAt(i);
            }
        }
        return str2;
    }
    private void deleteAllDuplicates(){
        File[] dupFiles = setDupFiles(filesForTest);
        deleteFile(dupFiles);
    }



    private void newFile() {
        if (currentFile==null) {
            showErrorMessage("No location selected for new file.","Select Location");
            return;
        }

        if (newFilePanel==null) {
            newFilePanel = new JPanel(new BorderLayout(3,3));

            JPanel southRadio = new JPanel(new GridLayout(1,0,2,2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add( newTypeFile );
            southRadio.add( newTypeDirectory );

            name = new JTextField(15);

            newFilePanel.add( new JLabel("Name"), BorderLayout.WEST );
            newFilePanel.add( name );
            newFilePanel.add( southRadio, BorderLayout.SOUTH );
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                newFilePanel,
                "Create File",
                JOptionPane.OK_CANCEL_OPTION);
        if (result==JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File( parentFile, name.getText() );
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                            (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            file +
                            "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void renameFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected to rename.","Select File");
            return;
        }

        String renameTo = JOptionPane.showInputDialog(gui, "New Name");
        if (renameTo!=null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                boolean renamed = currentFile.renameTo(new File(
                        currentFile.getParentFile(), renameTo));
                if (renamed) {
                    if (directory) {
                        // rename the node..

                        // delete the current node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);

                        // add a new node..
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be renamed.";
                    showErrorMessage(msg,"Rename Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void deleteFile() {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                "Are you sure you want to delete this file?",
                "Delete File",
                JOptionPane.ERROR_MESSAGE
        );
        if (result==JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                if (FileUtils.deleteQuietly(currentFile)) {
                    if (directory) {
                        // delete the node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be deleted.";
                    showErrorMessage(msg,"Delete Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void deleteFile(File currentFile) {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                "Are you sure you want to delete this file?",
                "Delete File",
                JOptionPane.ERROR_MESSAGE
        );
        if (result==JOptionPane.OK_OPTION) {
            try {
                System.out.println("currentFile: " + currentFile);
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                System.out.println("parentPath: " + parentPath);
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                System.out.println("parentNode: " + parentNode);

                boolean directory = currentFile.isDirectory();
                if (FileUtils.deleteQuietly(currentFile)) {
                    if (directory) {
                        // delete the node..
                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        treeModel.removeNodeFromParent(currentNode);
                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            currentFile +
                            "' could not be deleted.";
                    showErrorMessage(msg,"Delete Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }
    private void deleteFile(File[] files) {
        if (currentFile==null) {
            showErrorMessage("No file selected for deletion.","Select File");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                gui,
                "Are you sure you want to delete all duplicate files?",
                "Delete File",
                JOptionPane.ERROR_MESSAGE
        );
        if (result==JOptionPane.OK_OPTION) {
            for(int i=0;i<files.length;i++){
                if(i%2==1){
                    try {
                        System.out.println("currentFile: " + files[i]);
                        TreePath parentPath = findTreePath(files[i].getParentFile());
                        System.out.println("parentPath: " + parentPath);
                        DefaultMutableTreeNode parentNode =
                                (DefaultMutableTreeNode)parentPath.getLastPathComponent();
                        System.out.println("parentNode: " + parentNode);

                        boolean directory = files[i].isDirectory();
                        if (FileUtils.deleteQuietly(files[i])) {
                            if (directory) {
                                // delete the node..
                                TreePath currentPath = findTreePath(files[i]);
                                System.out.println(currentPath);
                                DefaultMutableTreeNode currentNode =
                                        (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                                treeModel.removeNodeFromParent(currentNode);
                            }

                            showChildren(parentNode);
                        } else {
                            String msg = "The file '" +
                                    currentFile +
                                    "' could not be deleted.";
                            showErrorMessage(msg,"Delete Failed");
                        }
                    } catch(Throwable t) {
                        showThrowable(t);
                    }
                }


            }
        }
        gui.repaint();
    }
    private void seprateFiles(){
        Path path1;
        String sepPath;
        String ext;
        String extPath;
        try{
            path1 = Paths.get(filesForTest[0].getParent()) ;
            sepPath = path1+"/SepratedFiles";
            Files.createDirectory(Paths.get(sepPath));
            for(File f : filesForTest){
                try{
                    ext = FilenameUtils.getExtension(f.getAbsolutePath());
                    extPath = sepPath+"/"+ext;
                    Files.createDirectory(Paths.get(extPath));
                }
                catch (Throwable e){
                    //                    showThrowable(e);
                }
            }
            for(File f : filesForTest){
                try{
                    if(f.isDirectory()){}
                    else{
                        File one = new File(f.getAbsolutePath());
                        ext = FilenameUtils.getExtension(f.getAbsolutePath());

                        File two = new File(sepPath+"/"+ext+"/"+f.getName());
                        Files.copy(one.toPath(),two.toPath());
                    }
                }
                catch (Throwable e){
                    //                    showThrowable(e);
                }
            }

        }
        catch (Throwable e ){
            //            showThrowable(e);
        }

    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
                gui,
                errorMessage,
                errorTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }
    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
                gui,
                t.toString(),
                t.getMessage(),
                JOptionPane.ERROR_MESSAGE
        );
        gui.repaint();
    }

    private void setTableData(final File[] files) {
        filesForTest = files;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel==null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);

                    // size adjustment to better account for icons
                    table.setRowHeight( icon.getIconHeight()+rowIconPadding );

                    setColumnWidth(0,-1);
                    setColumnWidth(3,60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4,-1);
                    setColumnWidth(5,-1);
                    setColumnWidth(6,-1);
                    setColumnWidth(7,-1);
                    setColumnWidth(8,-1);
                    setColumnWidth(9,-1);

                    cellSizesSet = true;
                }
            }
        });
    }
    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width<0) {
            // use the preferred width of the header..
            JLabel label = new JLabel( (String)tableColumn.getHeaderValue() );
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int)preferred.getWidth()+14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); //!!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }
    private void setFileDetails(File file) {
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());

        isFile.setSelected(file.isFile());

        JFrame f = (JFrame)gui.getTopLevelAncestor();
        if (f!=null) {
            f.setTitle(
                    APP_TITLE +
                            " :: " +
                            fileSystemView.getSystemDisplayName(file) );
        }

        gui.repaint();
    }
    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0,0);
    }
    private TreePath findTreePath(File find) {
        for (int ii=0; ii<tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            File nodeFile = (File)node.getUserObject();

            if (nodeFile.equals(find)) {
                return treePath;
            }
        }
        // not found!
        return null;
    }
    public static boolean copyFile(File from, File to) throws IOException {

        boolean created = to.createNewFile();

        if (created) {
            FileChannel fromChannel = null;
            FileChannel toChannel = null;
            try {
                fromChannel = new FileInputStream(from).getChannel();
                toChannel = new FileOutputStream(to).getChannel();

                toChannel.transferFrom(fromChannel, 0, fromChannel.size());

                // set the flags of the to the same as the from
                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
            } finally {
                if (fromChannel != null) {
                    fromChannel.close();
                }
                if (toChannel != null) {
                    toChannel.close();
                }
                return false;
            }
        }
        return created;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {

                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception weTried) {
                }
                JFrame frame = new JFrame(APP_TITLE);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                FileManager fileManager = new FileManager();
                frame.setContentPane(fileManager.getFrame());

                try {
                    URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                    URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                    ArrayList<Image> images = new ArrayList<Image>();
                    images.add( ImageIO.read(urlBig) );
                    images.add( ImageIO.read(urlSmall) );
                    frame.setIconImages(images);
                } catch(Exception weTried) {}

                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setMinimumSize(new Dimension(1500 ,800));
                frame.setVisible(true);

                fileManager.showRootFile();
            }
        });
    }
}

/** A TableModel to hold File[]. */
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
            "Icon",
            "File",
            "Path/name",
            "Size",
            "Last Modified",
            "R",
            "W",
            "E",
            "D",
            "F",
    };

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files.length;
    }

    public File getFile(int row) {
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}

/** A TreeCellRenderer for a File. */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        File file = (File)node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
