/*
 * Copyright (c) 2015, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of Salesforce.com, Inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.desk.android.sdk.widget;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.EditText;
import android.widget.TextView;

import com.desk.android.sdk.Desk;
import com.desk.android.sdk.config.BaseContactUsConfig;
import com.desk.android.sdk.error.IncompleteFormException;
import com.desk.android.sdk.identity.UserIdentity;
import com.desk.android.sdk.model.CreateCaseRequest;
import com.desk.android.sdk.model.CustomFieldProperties;
import com.desk.android.sdk.test.R;
import com.desk.android.sdk.util.DeskDefaultsRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static com.desk.android.sdk.util.TestUtils.getString;
import static com.desk.android.sdk.util.TestUtils.inflateView;
import static org.assertj.android.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ContactUsView}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ContactUsViewTest {

    private ContactUsView.FormListener mockFormListener;
    private ContactUsView contactUsView;

    @ClassRule
    public static DeskDefaultsRule resetRule = new DeskDefaultsRule();

    @Rule
    public final UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        mockFormListener = mock(ContactUsView.FormListener.class);
        contactUsView = getNewContactUsView();
    }

    @Test
    @UiThreadTest
    public void viewHasVerticalOrientation() throws Exception {
        assertThat(contactUsView).isVertical();
    }

    // region FormListener Tests

    @Test
    @UiThreadTest
    public void onFormValidCalledWhenFormValid() throws Exception {

        // this sets up ContactUsView to have textChangedListeners on email and subject
        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public String getSubject() {
                        // don't want a subject so the text changed listener works
                        return null;
                    }

                    @Override
                    public boolean isSubjectEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isUserNameEnabled() {
                        return true;
                    }
                });

        ContactUsView contactUsView = getNewContactUsView();
        contactUsView.setFormListener(mockFormListener);
        getFeedback(contactUsView).setText("Valid feedback");
        verify(mockFormListener, atLeast(1)).onFormValid();
    }

    @Test
    @UiThreadTest
    public void onFormValidCalledWhenFormInvalid() throws Exception {

        // this sets up ContactUsView to have textChangedListeners on email and subject
        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public String getSubject() {
                        // don't want a subject so the text changed listener works
                        return null;
                    }

                    @Override
                    public boolean isSubjectEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isUserNameEnabled() {
                        return true;
                    }
                });

        ContactUsView contactUsView = getNewContactUsView();
        contactUsView.setFormListener(mockFormListener);
        getFeedback(contactUsView).setText(""); // empty feedback
        verify(mockFormListener, atLeast(1)).onFormInvalid();
    }

    @Test
    @UiThreadTest
    public void clearFormListenerClearsListener() throws Exception {
        ContactUsView contactUsView = getNewContactUsView();
        contactUsView.setFormListener(mockFormListener);
        assertNotNull(contactUsView.getFormListener());
        contactUsView.clearFormListener();
        assertNull(contactUsView.getFormListener());
    }

    // endregion

    //region Hint Test
    @Test
    @UiThreadTest
    public void feedbackHintMatchesDefault() throws Exception {
        assertThat(getFeedback(contactUsView)).hasHint(getString(R.string.def_user_feedback_hint));
    }

    @Test
    @UiThreadTest
    public void feedbackHintMatchesValueFromLayoutAttrs() throws Exception {
        ContactUsView contactUsView = inflateView(R.layout.contact_us_view_with_attributes);
        assertThat(getFeedback(contactUsView)).hasHint(getString(R.string.test_attr_feedback_hint));
    }

    @Test
    @UiThreadTest
    public void feedbackHintMatchesValueFromStyle() throws Exception {
        ContactUsView contactUsView = inflateView(R.layout.contact_us_view_with_style);
        assertThat(getFeedback(contactUsView)).hasHint(getString(R.string.test_style_feedback_hint));
    }
    //endregion

    // region Focus Tests

    @Test
    @UiThreadTest
    public void userFeedbackHasFocusWhenEmptyAndEnabled() throws Exception {
        Desk.with(context)

                // set identity so email is populated
                .setIdentity(new UserIdentity.Builder("email@email.com").create())
                .setContactUsConfig(new BaseContactUsConfig(context) {

                    // disable user name so feedback is first visible field
                    @Override public boolean isUserNameEnabled() {
                        return false;
                    }

                    @Override public String getSubject() {
                        return "Populated";
                    }

                    // disable subject so feedback is first visible field
                    @Override public boolean isSubjectEnabled() {
                        return false;
                    }
                });
        EditText userFeedback = (EditText) getFeedback(getNewContactUsView());
        assertThat(userFeedback).isVisible();
        assertThat(userFeedback).isEmpty();
        assertThat(userFeedback).hasFocus();
    }

    // endregion

    // region Validation Tests

    @Test
    @UiThreadTest
    public void isFormValidReturnsFalseWhenEmailEmpty() throws Exception {

        // need to clear the identity
        clearIdentity();

        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText("Feedback");
        assertFalse(contactUsView.isFormValid());
    }

    @Test
    @UiThreadTest
    public void isFormValidReturnsFalseWhenEmailInvalid() throws Exception {

        // need to clear the identity
        clearIdentity();

        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText("Feedback");
        assertFalse(contactUsView.isFormValid());
    }

    @Test
    @UiThreadTest
    public void isFormValidReturnsFalseWhenSubjectEmpty() throws Exception {
        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public String getSubject() {
                        return null;
                    }
                });

        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText("Feedback");
        assertFalse(contactUsView.isFormValid());
    }

    @Test
    @UiThreadTest
    public void isFormValidReturnsFalseWhenFeedbackEmpty() throws Exception {
        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public boolean isSubjectEnabled() {
                        return true;
                    }
                });

        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText("");
        assertFalse(contactUsView.isFormValid());
    }

    @Test
    @UiThreadTest
    public void isFormValidReturnsTrueWhenValid() throws Exception {

        // need to clear the identity
        clearIdentity();

        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public boolean isSubjectEnabled() {
                        return true;
                    }
                });

        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText("Feedback");
        assertTrue(contactUsView.isFormValid());
    }

    // endregion

    // region getRequest Tests

    @Test
    @UiThreadTest
    public void getRequestReturnsValidRequest() throws Exception {
        final String subject = "Valid Subject";
        final String name = "Valid Name";
        final String email = "valid@email.com";
        final String feedback = "Valid Feedback";
        final String to = "to@email.com";
        final String key1 = "key1";
        final String value1 = "value1";
        final String key2 = "key2";
        final String value2 = "value2";
        Desk.with(context)
                .setContactUsConfig(new BaseContactUsConfig(context) {
                    @Override
                    public String getSubject() {
                        return subject;
                    }

                    @Override
                    public HashMap<String, CustomFieldProperties> getCustomFieldProperties() {
                        HashMap<String, CustomFieldProperties> properties = new HashMap<>();
                        properties.put(key1, new CustomFieldProperties.Builder(key1).value(value1).create());
                        properties.put(key2, new CustomFieldProperties.Builder(key2).value(value2).create());
                        return properties;
                    }
                })
                .setIdentity(new UserIdentity.Builder(email).name(name).create());
        ContactUsView contactUsView = getNewContactUsView();
        getFeedback(contactUsView).setText(feedback);
        CreateCaseRequest request = contactUsView.getRequest(to);
        assertEquals(request.getSubject(), subject);
        assertEquals(request.getName(), name);
        assertEquals(request.getFrom(), email);
        assertEquals(request.getTo(), to);
        assertEquals(request.getBody(), feedback);
        HashMap<String, String> customFields = request.getCustomFields();
        assertTrue(customFields.containsKey(key1));
        assertTrue(customFields.containsKey(key2));
        assertTrue(customFields.containsValue(value1));
        assertTrue(customFields.containsValue(value2));
    }

    @Test(expected = IncompleteFormException.class)
    @UiThreadTest
    public void getRequestThrowsIncompleteFormException() {
        ContactUsView contactUsView = getNewContactUsView();
        contactUsView.getRequest("to@email.com");
    }

    // endregion

    private TextView getFeedback(ContactUsView contactUsView) {
        return (TextView) contactUsView.findViewById(R.id.user_feedback);
    }

    private void clearIdentity() {
        Desk.with(context).setIdentity(null);
    }

    private ContactUsView getNewContactUsView() {
        return new ContactUsView(context);
    }
}