/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.impl.pvm.runtime.operation;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.pvm.runtime.ProcessInstanceStartContext;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

import java.util.List;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class PvmAtomicOperationProcessStartInitial extends PvmAtomicOperationActivityInstanceStart {

  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    return execution.getActivity();
  }

  protected String getEventName() {
    return ExecutionListener.EVENTNAME_START;
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {

    super.eventNotificationsCompleted(execution);

    ActivityImpl activity = execution.getActivity();
    ProcessDefinitionImpl processDefinition = execution.getProcessDefinition();

    ProcessInstanceStartContext processInstanceStartContext = execution.getProcessInstanceStartContext();
    if (processInstanceStartContext==null) {
      // The ProcessInstanceStartContext is set on the process instance / parent execution - grab it from there:
      PvmExecutionImpl executionToUse = execution;
      while (processInstanceStartContext==null) {
        executionToUse = execution.getParent();
        processInstanceStartContext = executionToUse.getProcessInstanceStartContext();
      }
    }

    if (activity== processInstanceStartContext.getInitial()) {

      processInstanceStartContext.initialStarted(execution);

      execution.disposeProcessInstanceStartContext();
      execution.performOperation(ACTIVITY_EXECUTE);

    } else {
      List<ActivityImpl> initialActivityStack = processDefinition.getInitialActivityStack(processInstanceStartContext.getInitial());
      int index = initialActivityStack.indexOf(activity);
      // starting the next one
      activity = initialActivityStack.get(index+1);

      // and search for the correct execution to set the Activity to
      PvmExecutionImpl executionToUse = execution;
      if (executionToUse.getActivity().isScope()) {
        executionToUse.setActive(false); // Deactivate since we jump to a node further down the hierarchy
        executionToUse = executionToUse.getExecutions().get(0);
      }
      executionToUse.setActivity(activity);
      executionToUse.performOperation(PROCESS_START_INITIAL);
    }
  }

  public String getCanonicalName() {
    return "process-start-initial";
  }

}
