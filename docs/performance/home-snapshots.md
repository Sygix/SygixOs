# HOME snapshot execution and ownership

`HomeStateReader.read()` is a suspend boundary. The Android implementation performs PackageManager, role and settings reads on IO; controller state remains confined to its owning Main scope.

Passive resume refreshes are replaceable. A new resume cancels the preceding passive refresh, and an explicit setup sequence cancels passive work before changing system state. Setup itself remains application-scoped and is not cancelled merely because its panel closes.

Every snapshot publication, including sequence finalization, uses the same request generation. A read that completes after a newer request cannot overwrite its result. Clearing the sequence busy flag does not depend on winning the snapshot publication race.

Regression coverage:
- no inline reads during construction/resume bursts;
- Android package reads off Main;
- cancellation of a preceding resume;
- final sequence read finishing after a newer resume result;
- existing setup, restoration and permission-failure behavior.

These checks prove scheduling and ordering, not a latency percentage. Repeated controlled Android traces are required to quantify a response-time gain. Smaller manual permission-gateway reads are a separate boundary and are not moved by this change.
