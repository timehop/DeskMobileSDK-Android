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

package com.desk.android.sdk.mvp.presenter.impl;

import com.desk.android.sdk.Desk;
import com.desk.android.sdk.jobqueue.JobManagerProvider;
import com.desk.android.sdk.jobqueue.PostChatMessage;
import com.desk.android.sdk.mvp.model.ChatMessageModel;
import com.desk.android.sdk.mvp.presenter.IChatPresenter;
import com.desk.android.sdk.mvp.usecase.CreateGuestCustomer;
import com.desk.android.sdk.mvp.usecase.EndChatSession;
import com.desk.android.sdk.mvp.usecase.SetUserStartedTyping;
import com.desk.android.sdk.mvp.usecase.SetUserStoppedTyping;
import com.desk.android.sdk.mvp.usecase.StartChatSession;
import com.desk.android.sdk.mvp.view.IChatView;
import com.desk.java.apiclient.model.chat.ChatSession;
import com.desk.java.apiclient.model.chat.GuestCustomer;
import com.desk.java.apiclient.service.RxChatService;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Matt Kranzler on 12/3/15.
 * Copyright (c) 2015 Desk.com. All rights reserved.
 */
public class ChatPresenter implements IChatPresenter {

    private RxChatService chatService;
    private String chatToken;
    private GuestCustomer guestCustomer;
    private ChatSession chatSession;

    public interface DestroyCallback {
        void onDestroyed();
    }

    private DestroyCallback destroyCallback;
    private IChatView view;

    public ChatPresenter(DestroyCallback destroyCallback) {
        this.destroyCallback = destroyCallback;
    }

    @Override public void attach(IChatView view) {
        this.view = view;
        init(view);
    }

    @Override public void userStartedTyping() {
        if (chatSession != null) {
            new SetUserStartedTyping(chatService)
                    .execute(guestCustomer.id, chatSession.id, chatToken, guestCustomer.token)
                    .subscribe(new Action1<Void>() {
                        @Override public void call(Void aVoid) {

                        }
                    });
        }
    }

    @Override public void userStoppedTyping() {
        if (chatSession != null) {
            new SetUserStoppedTyping(chatService)
                    .execute(guestCustomer.id, chatSession.id, chatToken, guestCustomer.token)
                    .subscribe(new Action1<Void>() {
                        @Override public void call(Void aVoid) {

                        }
                    });
        }
    }

    @Override public void handleNewMessage(String message) {
        ChatMessageModel chatMessage = new ChatMessageModel(message);
        JobManagerProvider.get(view.getContext()).addJobInBackground(new PostChatMessage(chatMessage, chatService,
                guestCustomer, chatToken, chatSession._links.caseLink.getLinkId()));
    }

    @Override public void detach(IChatView view) {
        view = null;
    }

    @Override public void startSession(String userName) {
        if (chatSession == null) {
            new CreateGuestCustomer(chatService, userName, chatToken).execute()
                    .flatMap(new Func1<GuestCustomer, Observable<ChatSession>>() {
                        @Override
                        public Observable<ChatSession> call(GuestCustomer info) {
                            guestCustomer = info;
                            return new StartChatSession(chatService, guestCustomer.id, chatToken, guestCustomer.token).execute();
                        }
                    })
                    .subscribe(
                            new Action1<ChatSession>() {
                                @Override
                                public void call(ChatSession info) {
                                    chatSession = info;
                                }
                            },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    // TODO handle
                                }
                            }
                    );
        }
    }

    @Override public void destroy() {
        if (chatSession != null) {
            new EndChatSession(chatService, guestCustomer.id, chatSession.id, chatToken, guestCustomer.token)
                    .execute()
                    .subscribe(new Action1<Void>() {
                                   @Override
                                   public void call(Void aVoid) {

                                   }
                               },
                            new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {

                                }
                            });
        }

        if (destroyCallback != null) {
            destroyCallback.onDestroyed();
        }
    }

    private void init(IChatView view) {
        Desk desk = Desk.with(view.getContext());
        chatService = desk.getRxClient().chatRx();
        chatToken = desk.getConfig().getChatToken();
    }
}
