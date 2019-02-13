package com.example.contract

import com.example.state.IOUState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class IOUContract : Contract {
    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "com.example.contract.IOUContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        tx.commands.forEach {
            when (it.value) {
                is Commands.Create -> {
                    requireThat {
                        // Generic constraints around the IOU transaction.
                        "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
                        "Only one output state should be created." using (tx.outputs.size == 1)
                        val out = tx.outputsOfType<IOUState>().single()
                        "The lender and the borrower cannot be the same entity." using (out.lender != out.borrower)
                        "All of the participants must be signers." using (it.signers.containsAll(out.participants.map { it.owningKey }))

                        // IOU-specific constraints.
                        "The IOU's value must be non-negative." using (out.value > 0)
                    }
                }
                is Commands.Settle -> {
                    requireThat {
                        "There should be one input IOU State" using (tx.inputs.size == 1)
                        "Only one output state should be provided" using (tx.outputs.size == 1)
                        val outIOU = tx.outputsOfType<IOUState>().single()
                        val inIOU = tx.inputsOfType<IOUState>().single()
                        "Lender should be same in both input and output states" using (inIOU.lender == outIOU.lender)
                        "Borrower should be same in both input and output states" using (inIOU.borrower == outIOU.borrower)
                        "Settle value should be less than or equal to total remaining amount to paid." using (outIOU.value >= 0)
                        "Linear ID of the input and output states should be same" using (outIOU.linearId == inIOU.linearId)
                    }
                }
            }
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
        class Settle : Commands
    }
}
