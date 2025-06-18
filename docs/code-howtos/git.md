# git

## Conflict Scenarios
- **T1.** Remote changed a field, local did not  
  → No conflict.  
  The local version remained unchanged, so the remote change can be safely applied.


- **T2.** Local changed a field, remote did not  
  → No conflict.  
  The remote version did not touch the field, so the local change is preserved.


- **T3.** Both local and remote changed the same field to the same value  
  → No conflict.  
  Although both sides changed the field, the result is identical—therefore, no conflict.


- **T4.** Both local and remote changed the same field to different values  
  → Conflict.  
  This is a true semantic conflict that requires resolution.


- **T5.** Local deleted a field, remote modified the same field  
  → Conflict.  
  One side deleted the field while the other updated it—this is contradictory.


- **T6.** Local modified a field, remote deleted it  
  → Conflict.  
  Similar to T5, one side deletes, the other edits—this is a conflict.


- **T7.** Local unchanged, remote deleted a field  
  → No conflict.  
  Local did not modify anything, so remote deletion is accepted.


- **T8.** Local changed field A, remote changed field B (within the same entry)  
  → No conflict.  
  Changes are on separate fields, so they can be merged safely.


- **T9.** Both changed the same entry, but only field order changed  
  → No conflict.  
  Field order is not semantically meaningful, so no conflict is detected.

- **T10.** Local modified entry A, remote modified entry B  
  → No conflict.  
  Modifications are on different entries, which are always safe to merge.


- **T11.** Remote added a new field, local did nothing  
  → No conflict.  
  Remote addition can be applied without issues.


- **T12.** Remote added a field, local also added the same field, but with different value  
  → Conflict.  
  One side added while the other side modified—there is a semantic conflict.


- **T13.** Local added a field, remote did nothing  
  → No conflict.  
  Safe to preserve the local addition.


- **T14.** Both added the same field with the same value  
  → No conflict.  
  Even though both sides added it, the value is the same—no need for resolution.


- **T15.** Both added the same field with different values  
  → Conflict.  
  The same field is introduced with different values, which creates a conflict.
