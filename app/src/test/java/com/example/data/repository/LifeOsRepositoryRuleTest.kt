package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LifeOsRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_and_observeTasks_authenticatedOwner_succeedsAndRespectsIdempotency() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = LifeOsRepository(firestore)

        val idemKey = "voice_cmd_${System.currentTimeMillis()}"
        val createResult1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "پرداخت قبض اینترنت",
                category = "مالی",
                priority = "HIGH",
                recurrenceRule = "MONTHLY",
                inputSource = "VOICE",
                idempotencyKey = idemKey
            )
        }
        assertTrue(createResult1.isSuccess)
        val taskId1 = createResult1.getOrThrow()

        // Duplicate submission with same idempotencyKey returns same taskId without duplicating
        val createResult2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "پرداخت قبض اینترنت",
                category = "مالی",
                priority = "HIGH",
                recurrenceRule = "MONTHLY",
                inputSource = "VOICE",
                idempotencyKey = idemKey
            )
        }
        assertEquals(taskId1, createResult2.getOrThrow())

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(aliceUid).first { list -> list.any { it.id == taskId1 } }
        }
        assertTrue(emittedTasks.any { it.id == taskId1 && it.title == "پرداخت قبض اینترنت" })
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LifeOsRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(title = "تسک خصوصی آلیس").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LifeOsRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTaskById(aliceUid, taskId)
        }
        assertTrue(crossReadResult.isFailure)
        val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = LifeOsRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks(aliceUid).first()
            }
            fail("Expected exception when observing tasks unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@personallifeos.test"
        const val BOB_EMAIL = "bob@personallifeos.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
