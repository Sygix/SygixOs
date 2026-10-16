# Activity integration test boundaries

Activity routing tests keep the actual Activity callbacks and Activity-owned ViewModels. The test application supplies a deterministic HOME snapshot, boot counter and store boundary through the application composition root. They do not depend on a mutable Android settings shadow or a concurrently updated persistence projection to prove that an intent was routed.

`MainDispatcherRule` owns and joins both Activity ViewModel scopes. The repeated boot-intent scenario checks that stop/resume retains the same live launcher ViewModel and reports two observations to a controlled recording port. Other store operations still delegate to the real preferences. `LauncherSystemPrefsTest` separately verifies actual DataStore persistence, including recording the same boot after disable/re-enable clears its prior observation.

`AndroidLauncherSystemGatewayTest` exercises the actual boot-count settings adapter separately, together with permission/error handling. The production factories keep the same Android gateway, shared accessibility check and real preference store; only the test application substitutes these boundaries.

This separates the routing/persistence contract from the Android adapter contract. It does not increase deadlines, retry a failing assertion, or claim that a specific Android settings-cache failure has been proven.
