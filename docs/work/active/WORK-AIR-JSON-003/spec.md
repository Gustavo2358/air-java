# CP6 W1B specification

AIR 04 §7 / 05 / 02 and binding 1.0.0 at normative pin 51b4d9a8ae0364232bd97103cd73a77e1a34996c already define Invoke. Transport coverage is missing, followed by an independent AirJson refusal of I-56. The same Publication must survive encode/decode and canonical bytes must be stable.

Use the [closure and authority](../../../quality/cp6-w1b-invoke.md). Structural validity plus semantic obligations is transportable; no obligation is discharged. Classify all other failures by actual ValidationIssue.Kind, total counts and traversal completion, never message parsing. No model/Validator semantics, version, dependency, public API, sibling, language interpretation, lowering or analysis changes.
