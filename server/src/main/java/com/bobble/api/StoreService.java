package com.bobble.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bobble.api.resources.Bobble;
import com.bobble.api.resources.BobbleAddRequest;
import com.bobble.api.resources.Status;

public class StoreService {
    ConcurrentLinkedQueue<Bobble> allBobbles;
    ConcurrentLinkedQueue<Bobble> shelf;
    Thread shelfThread;

    public void start() {
        if (allBobbles != null)
            return;

        allBobbles = new ConcurrentLinkedQueue<Bobble>();
        shelf = new ConcurrentLinkedQueue<Bobble>();
        shelfThread = new Thread(new ShelfRunnable(allBobbles, shelf));
        shelfThread.setDaemon(true);
        shelfThread.start();
    }

    public void bobbleAddRequest(BobbleAddRequest req) {
        shelf.add(new Bobble(req.getOrderId()));
    }

    public Collection<Bobble> getBobbles()
        throws InterruptedException {
        ArrayList<Bobble> bobbles = new ArrayList<Bobble>();
        for (Bobble bobble: allBobbles)
            bobbles.add(bobble.clone());
        return bobbles;
    }

    static final class ShelfRunnable implements Runnable {
        ConcurrentLinkedQueue<Bobble> allBobbles;
        ConcurrentLinkedQueue<Bobble> shelf;

        public ShelfRunnable(ConcurrentLinkedQueue<Bobble> allBobbles, ConcurrentLinkedQueue<Bobble> shelf) {
            this.allBobbles = allBobbles;
            this.shelf = shelf;
        }

        public void run() {
            try {
                runImpl();
            } catch (InterruptedException exc) {
            }
        }

        void runImpl() throws InterruptedException {
            while (true) {
                Object bobble = shelf.poll();
                if (bobble == null) {
                    Thread.sleep(10);
                    continue;
                }

                Bobble bobbleRequest = (Bobble) bobble;
                switch (bobbleRequest.getStatus()) {
                    case NEW_ORDER:
                        bobbleRequest.setStatus(Status.RECEIVED);
                        allBobbles.add(bobbleRequest);
                        shelf.add(bobbleRequest);
                        break;
                    case RECEIVED:
                        Thread.sleep(400);
                        bobbleRequest.setStatus(Status.PROCESSING);
                        shelf.add(bobbleRequest);
                        break;
                    case PROCESSING:
                        Thread.sleep(1000);
                        bobbleRequest.setStatus(Status.READY);
                        break;
				default:
					break;
                }
            }
        }
    }
}