package com.kwunai.player.core

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject


class PlayerEmit {

    private val subject: Subject<PlayerCommand> = PublishSubject.create()

    fun subject(): Subject<PlayerCommand> = subject

    fun onNext(command: PlayerCommand) {
        subject.onNext(command)
    }
}