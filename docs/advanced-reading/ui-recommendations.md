# Recommendations for UI design

* [Designing More Efficient Forms: Structure, Inputs, Labels and Actions](https://uxplanet.org/designing-more-efficient-forms-structure-inputs-labels-and-actions-e3a47007114f)
* [Input form label alignment top or left?](https://ux.stackexchange.com/questions/8480/input-form-label-alignment-top-or-left)
  * For a usual form, place the label above the text field
  * If the user uses the form often to edit fields, then it might make sense to switch to left-aligned labels

## Designing GUI Confirmation dialogs

1. Avoid asking questions
2. Be as concise as possible
3. Identify the item at risk
4. Name your buttons for the actions

More information: [http://ux.stackexchange.com/a/768](http://ux.stackexchange.com/a/768)

## Form validation

* Only validate input after leaving the field \(or after the user stopped typing for some time\)
* The user shouldn't be able to submit the form if there are errors
* However, disabling the submit button in case there are errors is also not optimal. Instead, clicking the submit button should highlight the errors.
* Empty required files shouldn't be marked as invalid until the user a\) tries to submit the form or b\) focused the field, deleted it contents and then left the field \(see [Example](https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_required)
* Ideally, the error message should be shown below the text field and not as a tooltip \(so that users quickly understand what's the problem\). For example as here [in Boostrap](https://mdbootstrap.com/docs/jquery/forms/validation/?#custom-styles)

