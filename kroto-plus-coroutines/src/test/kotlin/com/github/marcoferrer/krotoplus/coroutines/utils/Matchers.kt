/*
 * Copyright 2019 Kroto+ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.marcoferrer.krotoplus.coroutines.utils

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.mockk.MockKMatcherScope


fun MockKMatcherScope.matchStatus(status: Status, message: String? = null) =
    match<Throwable> { sre ->
        // Deliberately not using Status.fromThrowable so that we dont have to
        // worry about false position for matching status unknown
        val actualCode = (sre as? StatusException)?.status?.code
            ?: (sre as? StatusRuntimeException)?.status?.code

        val matchesMessage = message?.let { it == sre.message } ?: true

        actualCode == status.code && matchesMessage
    }