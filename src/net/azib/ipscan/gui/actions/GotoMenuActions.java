/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.gui.actions;

import net.azib.ipscan.config.Labels;
import net.azib.ipscan.core.ScanningResult;
import net.azib.ipscan.core.ScanningResult.ResultType;
import net.azib.ipscan.core.ScanningResultList;
import net.azib.ipscan.gui.FindDialog;
import net.azib.ipscan.gui.InputDialog;
import net.azib.ipscan.gui.ResultTable;
import net.azib.ipscan.gui.StatusBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;

/**
 * GotoActions
 *
 * @author Anton Keks
 */
public class GotoMenuActions {

	static class NextHost implements Listener {

		final ResultTable resultTable;
		final ResultType whatToSearchFor;
		
		NextHost(ResultTable resultTable, ResultType whatToSearchFor) {
			this.resultTable = resultTable;
			this.whatToSearchFor = whatToSearchFor;
		}
		
		protected int inc(int i) {
			return i+1;
		}
		
		protected int startIndex() {
			return resultTable.getSelectionIndex();
		}

		public final void handleEvent(Event event) {
			ScanningResultList results = resultTable.getScanningResults();
			
			int numElements = resultTable.getItemCount();
			int startIndex = startIndex();
			
			for (int i = inc(startIndex); i < numElements && i >= 0; i = inc(i)) {
				ScanningResult scanningResult = results.getResult(i);
				
				if (whatToSearchFor.matches(scanningResult.getType())) {
					resultTable.setSelection(i);
					resultTable.setFocus();
					return;
				}
				
			}
						
			// rewind
			if (startIndex >= 0 && startIndex < numElements) {
				resultTable.deselectAll();
				handleEvent(event);
			}
		}

	}
	
	static class PrevHost extends NextHost {

		public PrevHost(ResultTable resultTable, ResultType whatToSearchFor) {
			super(resultTable, whatToSearchFor);
		}

		protected int inc(int i) {
			return i-1;
		}

		protected int startIndex() {
			int curIndex = resultTable.getSelectionIndex();
			return curIndex >= 0 ? curIndex : resultTable.getItemCount();
		}
	}
	
	public static final class NextAliveHost extends NextHost {
		public NextAliveHost(ResultTable resultTable) {
			super(resultTable, ResultType.ALIVE);
		}
	}
	
	public static final class NextDeadHost extends NextHost {
		public NextDeadHost(ResultTable resultTable) {
			super(resultTable, ResultType.DEAD);
		}
	}
	
	public static final class NextHostWithInfo extends NextHost {
		public NextHostWithInfo(ResultTable resultTable) {
			super(resultTable, ResultType.WITH_PORTS);
		}
	}
	
	public static final class PrevAliveHost extends PrevHost {
		public PrevAliveHost(ResultTable resultTable) {
			super(resultTable, ResultType.ALIVE);
		}
	}
	
	public static final class PrevDeadHost extends PrevHost {
		public PrevDeadHost(ResultTable resultTable) {
			super(resultTable, ResultType.DEAD);
		}
	}
	
	public static final class PrevHostWithInfo extends PrevHost {
		public PrevHostWithInfo(ResultTable resultTable) {
			super(resultTable, ResultType.WITH_PORTS);
		}
	}
	
	public static final class Find implements Listener {

		private final ResultTable resultTable;
		private final StatusBar statusBar;
		private String lastText = "";

		public Find(StatusBar statusBar, ResultTable resultTable) {
			this.statusBar = statusBar;
			this.resultTable = resultTable;
		}
		
		public void handleEvent(Event event) {
			InputDialog dialog = new InputDialog(Labels.getLabel("title.find"), Labels.getLabel("text.find"));
			String text = dialog.open(lastText, Labels.getLabel("button.find.next"));
			if (text == null) {
				return;
			}
			lastText = text;
			ArrayList<Integer> foundMatches = findMatches(text);
			int selectionIndex = 0;

			if(foundMatches.size() == 0){
				MessageBox messageBox = new MessageBox(statusBar.getShell(), SWT.OK | SWT.ICON_INFORMATION);
				messageBox.setText(Labels.getLabel("title.find"));
				messageBox.setMessage(Labels.getLabel("text.find.notFound"));
				messageBox.open();
				return;
			}

			while(true){
				FindDialog find = new FindDialog(Labels.getLabel("title.find"), foundMatches.size() + " " + Labels.getLabel("text.found"));
				resultTable.setSelection(foundMatches.get(selectionIndex));
				resultTable.setFocus();
				int flag = find.open(selectionIndex + 1, foundMatches.size());

				if(flag == 1 && selectionIndex > 0){
					selectionIndex--;
				}
				else if (flag == 0 && selectionIndex < foundMatches.size()-1) {
					selectionIndex++;
				}
				else if(flag == -1){
					break;
				}
			}
		}

		private void findText(String text, Shell activeShell) {
			ScanningResultList results = resultTable.getScanningResults();
			
			int startIndex = resultTable.getSelectionIndex() + 1;
			
			int foundIndex = results.findText(text, startIndex);					
			
			if (foundIndex >= 0) {
				// if found, then select and finish
				resultTable.setSelection(foundIndex);
				resultTable.setFocus();
				return;
			}
			
			if (startIndex > 0) {
				// if started not from the beginning, offer to restart				
				MessageBox messageBox = new MessageBox(activeShell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
				messageBox.setText(Labels.getLabel("title.find"));
				messageBox.setMessage(Labels.getLabel("text.find.notFound") + "\n" + Labels.getLabel("text.find.restart"));
				if (messageBox.open() == SWT.YES) {
					resultTable.deselectAll();
					findText(text, activeShell);
				}
			}
			else {
				// searching is finished, nothing was found				
				MessageBox messageBox = new MessageBox(activeShell, SWT.OK | SWT.ICON_INFORMATION);
				messageBox.setText(Labels.getLabel("title.find"));
				messageBox.setMessage(Labels.getLabel("text.find.notFound"));
				messageBox.open();
			}


		}

		/**
		 * Makes an arraylist with a list of all indexes which match the input
		 *
		 * @return arraylist of integers
		 */
		private ArrayList<Integer> findMatches(String text){
			ScanningResultList results = resultTable.getScanningResults();

			ArrayList<Integer> foundMatches = new ArrayList<>();
			int lastIndex = 0;
			while(true){
				if(results.findText(text, lastIndex) == -1){
					break;
				}
				lastIndex = results.findText(text, lastIndex);
				foundMatches.add(lastIndex);
				lastIndex++;
			}

			return foundMatches;
		}
	}	
	
}
