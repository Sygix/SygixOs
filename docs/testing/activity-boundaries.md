# Activity integration test boundaries

Activity routing tests keep the actual Activity callbacks, Activity-owned ViewModels and DataStore persistence. The test application supplies a deterministic HOME snapshot and boot counter through the application composition root. They do not depend on a mutable Android settings shadow to supply hardware state during lifecycle transitions.

`MainDispatcherRule` owns and joins both Activity ViewModel scopes. The repeated boot-intent scenario also checks that stop/resume retains the same live launcher ViewModel. Clearing and restoring the recorded observation still goes through the public preference API.

`AndroidLauncherSystemGatewayTest` exercises the actual boot-count settings adapter separately, together with permission/error handling. The production factory continues to construct the same Android gateway with the shared accessibility check; only the test application overrides its counter input.

This separates the routing/persistence contract from the Android adapter contract. It does not increase deadlines, retry a failing assertion, or claim that a specific Android settings-cache failure has been proven.
